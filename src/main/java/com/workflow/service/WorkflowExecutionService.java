package com.workflow.service;

import com.workflow.exception.WorkflowException;
import com.workflow.model.Task;
import com.workflow.model.TaskStatus;
import com.workflow.model.Workflow;
import com.workflow.model.WorkflowStatus;
import com.workflow.repository.TaskRepository;
import com.workflow.repository.WorkflowRepository;
import com.workflow.util.ULIDGenerator;
import com.workflow.util.WorkflowDAG;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@CacheConfig(cacheNames = "workflows")
public class WorkflowExecutionService {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private WorkflowDAGService dagService;

    @Autowired
    private ULIDGenerator ulidGenerator;

    // In-memory cache for running workflows
    private final Map<String, WorkflowExecution> runningWorkflows = new ConcurrentHashMap<>();

    // Executor service for parallel task execution
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Start the execution of a workflow by ID.
     *
     * @param workflowId the ID of the workflow to run
     * @return the execution ID
     * @throws WorkflowException if the workflow cannot be started
     */
    @Transactional
    public String runWorkflow(String workflowId) throws WorkflowException {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowException("Workflow not found: " + workflowId));

        // Ensure workflow is not already running
        if (WorkflowStatus.RUNNING.equals(workflow.getStatus())) {
            throw new WorkflowException("Workflow is already running: " + workflowId);
        }

        // Generate execution ID
        String executionId = ulidGenerator.generate();

        // Set workflow status to RUNNING
        workflow.setStatus(WorkflowStatus.RUNNING);
        workflow.setStartTime(LocalDateTime.now());
        workflow.setEndTime(null);
        workflowRepository.save(workflow);

        // Get all tasks for the workflow
        List<Task> tasks = taskRepository.findByWorkflowId(workflowId);

        // Reset all tasks to PENDING status
        tasks.forEach(task -> {
            task.setStatus(TaskStatus.PENDING);
            task.setStartTime(null);
            task.setEndTime(null);
            taskRepository.save(task);
        });

        // Build DAG for the workflow
        WorkflowDAG dag = dagService.buildWorkflowDAG(workflow, tasks);

        // Create workflow execution object
        WorkflowExecution execution = new WorkflowExecution(
                executionId,
                workflow,
                tasks,
                dag,
                WorkflowStatus.RUNNING,
                LocalDateTime.now()
        );

        // Store in running workflows map
        runningWorkflows.put(executionId, execution);

        // Start execution asynchronously
        executeWorkflowAsync(executionId);

        return executionId;
    }

    /**
     * Execute a workflow asynchronously.
     *
     * @param executionId the execution ID
     */
    @Async
    public void executeWorkflowAsync(String executionId) {
        try {
            executeWorkflow(executionId);
        } catch (Exception e) {
            log.error("Error executing workflow {}: {}", executionId, e.getMessage(), e);
            handleWorkflowExecutionError(executionId, e);
        }
    }

    /**
     * Execute a workflow.
     *
     * @param executionId the execution ID
     */
    private void executeWorkflow(String executionId) {
        WorkflowExecution execution = runningWorkflows.get(executionId);
        if (execution == null) {
            log.error("Workflow execution not found: {}", executionId);
            return;
        }

        Workflow workflow = execution.getWorkflow();
        WorkflowDAG dag = execution.getDag();

        try {
            log.info("Starting workflow execution: {} ({})", workflow.getName(), executionId);

            // Start with root tasks
            Set<String> rootTaskIds = dag.getStartNodes();
            List<CompletableFuture<Void>> rootTaskFutures = new ArrayList<>();

            // Execute root tasks in parallel
            for (String taskId : rootTaskIds) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(
                        () -> executeTask(executionId, taskId, dag),
                        executorService
                );
                rootTaskFutures.add(future);
            }

            // Wait for all root tasks to complete
            CompletableFuture.allOf(rootTaskFutures.toArray(new CompletableFuture[0])).join();

            // Determine final workflow status
            boolean anyTaskFailed = execution.getTasks().stream()
                    .anyMatch(task -> TaskStatus.FAILURE.equals(task.getStatus()) &&
                            Boolean.TRUE.equals(task.getFailWorkflowOnError()));

            boolean allTasksCompleted = execution.getTasks().stream()
                    .allMatch(task -> isTaskCompleted(task.getStatus()));

            WorkflowStatus finalStatus;
            if (anyTaskFailed) {
                finalStatus = WorkflowStatus.FAILURE;
            } else if (allTasksCompleted) {
                finalStatus = WorkflowStatus.SUCCESS;
            } else {
                finalStatus = WorkflowStatus.FAILURE; // Some tasks not completed
            }

            // Update workflow status
            workflow.setStatus(finalStatus);
            workflow.setEndTime(LocalDateTime.now());
            workflowRepository.save(workflow);

            // Update execution status
            execution.setStatus(finalStatus);
            execution.setEndTime(LocalDateTime.now());

            // Cache the execution result if successful or failed (completed)
            if (finalStatus == WorkflowStatus.SUCCESS || finalStatus == WorkflowStatus.FAILURE) {
                cacheWorkflowExecution(executionId, execution);
            }

            log.info("Workflow execution completed: {} ({}) with status: {}",
                    workflow.getName(), executionId, finalStatus);

        } catch (Exception e) {
            log.error("Error in workflow execution: {} ({}): {}",
                    workflow.getName(), executionId, e.getMessage(), e);
            handleWorkflowExecutionError(executionId, e);
        } finally {
            // Remove from running workflows
            runningWorkflows.remove(executionId);
        }
    }

    /**
     * Execute a single task and its downstream tasks.
     *
     * @param executionId the execution ID
     * @param taskId the task ID
     * @param dag the workflow DAG
     */
    private void executeTask(String executionId, String taskId, WorkflowDAG dag) {
        WorkflowExecution execution = runningWorkflows.get(executionId);
        if (execution == null) {
            log.error("Workflow execution not found: {}", executionId);
            return;
        }

        // Find the task
        Task task = execution.getTasks().stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .findFirst()
                .orElse(null);

        if (task == null) {
            log.error("Task not found in workflow execution: {} ({})", taskId, executionId);
            return;
        }

        try {
            // Check if task can be executed (all dependencies completed successfully)
            boolean canExecute = canExecuteTask(task, dag, execution.getTasks());

            if (!canExecute) {
                log.info("Skipping task due to dependency failures: {} ({})", task.getName(), taskId);
                task.setStatus(TaskStatus.SKIPPED);
                taskRepository.save(task);
                return;
            }

            // Check preconditions
            boolean preconditionsMet = checkPreconditions(task);
            if (!preconditionsMet) {
                log.info("Skipping task due to preconditions not met: {} ({})", task.getName(), taskId);
                task.setStatus(TaskStatus.SKIPPED);
                taskRepository.save(task);
                return;
            }

            // Execute the task
            log.info("Executing task: {} ({})", task.getName(), taskId);
            task.setStatus(TaskStatus.RUNNING);
            task.setStartTime(LocalDateTime.now());
            taskRepository.save(task);

            // Perform the actual task execution
            boolean taskSuccess = taskExecutionService.executeTask(task);

            // Update task status
            if (taskSuccess) {
                task.setStatus(TaskStatus.SUCCESS);
            } else {
                task.setStatus(TaskStatus.FAILURE);
            }

            task.setEndTime(LocalDateTime.now());
            taskRepository.save(task);

            // If task completed successfully, execute downstream tasks
            if (taskSuccess) {
                Set<String> downstreamTasks = dag.getOutgoingEdges(taskId);
                List<CompletableFuture<Void>> downstreamFutures = new ArrayList<>();

                for (String downstreamTaskId : downstreamTasks) {
                    // Check if all upstream tasks of the downstream task are completed
                    Set<String> upstreamTasks = dag.getIncomingEdges(downstreamTaskId);
                    boolean allUpstreamCompleted = upstreamTasks.stream()
                            .allMatch(upstreamId -> isTaskCompleted(getTaskStatus(upstreamId, execution.getTasks())));

                    if (allUpstreamCompleted) {
                        CompletableFuture<Void> future = CompletableFuture.runAsync(
                                () -> executeTask(executionId, downstreamTaskId, dag),
                                executorService
                        );
                        downstreamFutures.add(future);
                    }
                }

                // Wait for all immediate downstream tasks to complete
                CompletableFuture.allOf(downstreamFutures.toArray(new CompletableFuture[0])).join();
            }

        } catch (Exception e) {
            log.error("Error executing task: {} ({}): {}", task.getName(), taskId, e.getMessage(), e);
            task.setStatus(TaskStatus.FAILURE);
            task.setEndTime(LocalDateTime.now());
            taskRepository.save(task);
        }
    }

    /**
     * Check if a task can be executed based on its dependencies.
     *
     * @param task the task to check
     * @param dag the workflow DAG
     * @param allTasks all tasks in the workflow
     * @return true if the task can be executed
     */
    private boolean canExecuteTask(Task task, WorkflowDAG dag, List<Task> allTasks) {
        Set<String> dependencies = dag.getIncomingEdges(task.getTaskId());

        // If task has no dependencies, it can always execute
        if (dependencies.isEmpty()) {
            return true;
        }

        // Check if all dependencies completed successfully
        for (String dependencyId : dependencies) {
            TaskStatus dependencyStatus = getTaskStatus(dependencyId, allTasks);

            // If dependency failed, check if task is configured to run on failure
            if (TaskStatus.FAILURE.equals(dependencyStatus)) {
                // Check if this task should execute even if dependencies fail
                if (Boolean.TRUE.equals(task.getForceExecution())) {
                    continue; // Allow execution despite dependency failure
                }
                return false; // Don't execute if dependency failed
            }

            // If dependency is not completed successfully, can't execute yet
            if (!TaskStatus.SUCCESS.equals(dependencyStatus)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if task preconditions are met.
     *
     * @param task the task to check
     * @return true if preconditions are met
     */
    private boolean checkPreconditions(Task task) {
        if (task.getPreconditions() == null || task.getPreconditions().isEmpty()) {
            return true; // No preconditions, always satisfied
        }

        // Evaluate each precondition
        Object input = task.getInputParameters(); // Could be parameters or anything needed for evaluation

        for (Predicate<Object> precondition : task.getPreconditions()) {
            if (!precondition.test(input)) {
                return false; // Precondition failed
            }
        }

        return true; // All preconditions passed
    }

    /**
     * Handle workflow execution error.
     *
     * @param executionId the execution ID
     * @param exception the exception that occurred
     */
    private void handleWorkflowExecutionError(String executionId, Exception exception) {
        WorkflowExecution execution = runningWorkflows.get(executionId);
        if (execution == null) {
            log.error("Workflow execution not found while handling error: {}", executionId);
            return;
        }

        Workflow workflow = execution.getWorkflow();
        workflow.setStatus(WorkflowStatus.FAILURE);
        workflow.setEndTime(LocalDateTime.now());
        workflowRepository.save(workflow);

        execution.setStatus(WorkflowStatus.FAILURE);
        execution.setEndTime(LocalDateTime.now());
        execution.setError(exception.getMessage());

        // Cache the failed execution result
        cacheWorkflowExecution(executionId, execution);

        // Clean up
        runningWorkflows.remove(executionId);
    }

    /**
     * Get the status of a workflow execution.
     *
     * @param executionId the execution ID
     * @return the workflow execution status, or null if not found
     */
    @Cacheable(key = "#executionId", unless = "#result == null")
    public WorkflowExecutionStatus getWorkflowExecutionStatus(String executionId) {
        // First check in-memory running workflows
        WorkflowExecution execution = runningWorkflows.get(executionId);

        if (execution != null) {
            return createExecutionStatus(execution);
        }

        // Not found in running workflows, check cache (handled by @Cacheable)
        // If this method returns null, it will try to fetch from the cache

        return null;
    }

    /**
     * Cache a completed workflow execution.
     *
     * @param executionId the execution ID
     * @param execution the workflow execution to cache
     * @return the cached workflow execution status
     */
    @CachePut(key = "#executionId")
    public WorkflowExecutionStatus cacheWorkflowExecution(String executionId, WorkflowExecution execution) {
        return createExecutionStatus(execution);
    }

    /**
     * Create a workflow execution status object.
     *
     * @param execution the workflow execution
     * @return the workflow execution status
     */
    private WorkflowExecutionStatus createExecutionStatus(WorkflowExecution execution) {
        List<TaskExecutionStatus> taskStatuses = execution.getTasks().stream()
                .map(task -> new TaskExecutionStatus(
                        task.getTaskId(),
                        task.getName(),
                        task.getStatus(),
                        task.getStartTime(),
                        task.getEndTime(),
                        task.getInputParameters(),
                        task.getOutputParameters()
                ))
                .collect(Collectors.toList());

        return new WorkflowExecutionStatus(
                execution.getExecutionId(),
                execution.getWorkflow().getWorkflowId(),
                execution.getWorkflow().getName(),
                execution.getStatus(),
                execution.getStartTime(),
                execution.getEndTime(),
                taskStatuses,
                execution.getError()
        );
    }

    /**
     * Clear the cache for a workflow execution.
     *
     * @param executionId the execution ID to clear from cache
     */
    @CacheEvict(key = "#executionId")
    public void clearExecutionCache(String executionId) {
        log.info("Cleared cache for workflow execution: {}", executionId);
    }

    /**
     * Get the status of a task by ID.
     *
     * @param taskId the task ID
     * @param tasks the list of all tasks
     * @return the task status
     */
    private TaskStatus getTaskStatus(String taskId, List<Task> tasks) {
        return tasks.stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .findFirst()
                .map(Task::getStatus)
                .orElse(TaskStatus.PENDING);
    }

    /**
     * Check if a task is considered completed (either success, failure, or skipped).
     *
     * @param status the task status
     * @return true if the task is completed
     */
    private boolean isTaskCompleted(TaskStatus status) {
        return status == TaskStatus.SUCCESS ||
                status == TaskStatus.FAILURE ||
                status == TaskStatus.SKIPPED;
    }

    /**
     * Workflow execution class for tracking execution state.
     */
    public static class WorkflowExecution {
        private final String executionId;
        private final Workflow workflow;
        private final List<Task> tasks;
        private final WorkflowDAG dag;
        private WorkflowStatus status;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        private String error;

        public WorkflowExecution(String executionId, Workflow workflow, List<Task> tasks,
                                 WorkflowDAG dag, WorkflowStatus status, LocalDateTime startTime) {
            this.executionId = executionId;
            this.workflow = workflow;
            this.tasks = tasks;
            this.dag = dag;
            this.status = status;
            this.startTime = startTime;
        }

        // Getters and setters
        public String getExecutionId() { return executionId; }
        public Workflow getWorkflow() { return workflow; }
        public List<Task> getTasks() { return tasks; }
        public WorkflowDAG getDag() { return dag; }
        public WorkflowStatus getStatus() { return status; }
        public void setStatus(WorkflowStatus status) { this.status = status; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    /**
     * Workflow execution status class for API responses.
     */
    public static class WorkflowExecutionStatus {
        private final String executionId;
        private final String workflowId;
        private final String workflowName;
        private final WorkflowStatus status;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final List<TaskExecutionStatus> tasks;
        private final String error;

        public WorkflowExecutionStatus(String executionId, String workflowId, String workflowName,
                                       WorkflowStatus status, LocalDateTime startTime,
                                       LocalDateTime endTime, List<TaskExecutionStatus> tasks,
                                       String error) {
            this.executionId = executionId;
            this.workflowId = workflowId;
            this.workflowName = workflowName;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.tasks = tasks;
            this.error = error;
        }

        // Getters
        public String getExecutionId() { return executionId; }
        public String getWorkflowId() { return workflowId; }
        public String getWorkflowName() { return workflowName; }
        public WorkflowStatus getStatus() { return status; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public List<TaskExecutionStatus> getTasks() { return tasks; }
        public String getError() { return error; }
    }

    /**
     * Task execution status class for API responses.
     */
    public static class TaskExecutionStatus {
        private final String taskId;
        private final String taskName;
        private final TaskStatus status;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final Map<String, Object> inputParameters;
        private final Map<String, Object> outputParameters;

        public TaskExecutionStatus(String taskId, String taskName, TaskStatus status,
                                   LocalDateTime startTime, LocalDateTime endTime,
                                   Map<String, Object> inputParameters,
                                   Map<String, Object> outputParameters) {
            this.taskId = taskId;
            this.taskName = taskName;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.inputParameters = inputParameters;
            this.outputParameters = outputParameters;
        }

        // Getters
        public String getTaskId() { return taskId; }
        public String getTaskName() { return taskName; }
        public TaskStatus getStatus() { return status; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public Map<String, Object> getInputParameters() { return inputParameters; }
        public Map<String, Object> getOutputParameters() { return outputParameters; }
    }
}