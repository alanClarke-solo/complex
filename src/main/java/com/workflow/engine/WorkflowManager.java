package com.workflow.engine;

import com.workflow.model.*;
import com.workflow.repository.*;
import com.workflow.service.TaskExecutorService;
import com.workflow.util.JsonFileReader;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class WorkflowManager {
    private final WorkflowRepository workflowRepository;
    private final TaskRepository taskRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final TaskExecutionRepository taskExecutionRepository;
    private final TaskExecutorService taskExecutorService;
    private final EventBus eventBus;
    private final JsonFileReader jsonFileReader;
    
    private final Map<UUID, WorkflowExecution> runningWorkflows = new ConcurrentHashMap<>();
    private final Map<UUID, WorkflowDAG> workflowDAGs = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    private String workflowsFolder;
    
    @PostConstruct
    public void init() {
        // Load workflows from the configuration folder
        loadWorkflowsFromFolder();
        
        // Subscribe to workflow interruption events
        eventBus.subscribe("workflow.interrupt", this::handleWorkflowInterruptEvent);
    }
    
    private void loadWorkflowsFromFolder() {
        try (Stream<Path> paths = Files.walk(Paths.get(workflowsFolder))) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(this::loadWorkflowFromFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load workflows from folder", e);
        }
    }
    
    private void loadWorkflowFromFile(Path filePath) {
        try {
            WorkflowDefinition definition = jsonFileReader.readWorkflowDefinition(filePath);
            saveWorkflowDefinition(definition);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load workflow from file: " + filePath, e);
        }
    }
    
    @Transactional
    private void saveWorkflowDefinition(WorkflowDefinition definition) {
        Workflow workflow = definition.getWorkflow();
        List<Task> tasks = definition.getTasks();
        
        workflowRepository.save(workflow);
        taskRepository.saveAll(tasks);
        
        // Validate DAG structure
        WorkflowDAG dag = buildDAG(workflow.getWorkflowId(), tasks);
        if (dag.hasCycle()) {
            throw new IllegalStateException("Workflow " + workflow.getWorkflowId() + " contains cycles");
        }
    }
    
    private WorkflowDAG buildDAG(String workflowId, List<Task> tasks) {
        // This implementation would read task dependencies from the task definitions
        // and build the DAG accordingly
        WorkflowDAG dag = new WorkflowDAG();
        
        // Add all tasks as nodes
        for (Task task : tasks) {
            dag.addNode(task.getTaskId(), task);
        }
        
        // Add edges based on task dependencies (simplified for clarity)
        // In a real implementation, this would parse the dependencies from task properties
        
        return dag;
    }
    
    @Transactional
    @Cacheable(value = "workflowExecutions", key = "#executionId")
    public WorkflowExecution startWorkflow(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
        
        List<Task> tasks = taskRepository.findByWorkflowId(workflowId);
        WorkflowDAG dag = buildDAG(workflowId, tasks);
        
        // Create workflow execution record
        WorkflowExecution execution = new WorkflowExecution();
        execution.setExecutionId(UUID.randomUUID());
        execution.setWorkflowId(workflowId);
        execution.setStatus(WorkflowStatus.STARTING);
        execution.setStartTime(LocalDateTime.now());
        
        workflowExecutionRepository.save(execution);
        
        // Store DAG for this execution
        workflowDAGs.put(execution.getExecutionId(), dag);
        runningWorkflows.put(execution.getExecutionId(), execution);
        
        // Start workflow execution asynchronously
        executorService.submit(() -> executeWorkflow(execution.getExecutionId(), dag));
        
        return execution;
    }
    
    private void executeWorkflow(UUID executionId, WorkflowDAG dag) {
        try {
            WorkflowExecution execution = runningWorkflows.get(executionId);
            
            // Update workflow status to RUNNING
            execution.setStatus(WorkflowStatus.RUNNING);
            workflowExecutionRepository.save(execution);
            
            // Start with the root tasks
            Set<String> startNodeIds = dag.getStartNodes();
            Map<String, TaskExecution> completedTasks = new HashMap<>();
            
            for (String taskId : startNodeIds) {
                executeTask(executionId, taskId, dag, completedTasks);
            }
            
            // Check if all mandatory tasks completed successfully
            boolean allMandatoryTasksSucceeded = true;
            // Logic to check if all mandatory tasks completed successfully
            
            // Update workflow execution status
            execution.setStatus(allMandatoryTasksSucceeded ? WorkflowStatus.SUCCESS : WorkflowStatus.FAILURE);
            execution.setEndTime(LocalDateTime.now());
            workflowExecutionRepository.save(execution);
            
            // Remove from running workflows
            runningWorkflows.remove(executionId);
            workflowDAGs.remove(executionId);
            
        } catch (Exception e) {
            handleWorkflowExecutionError(executionId, e);
        }
    }
    
    private void executeTask(UUID executionId, String taskId, WorkflowDAG dag, 
                            Map<String, TaskExecution> completedTasks) {
        Task task = dag.getTask(taskId);
        
        // Create task execution record
        TaskExecution taskExecution = new TaskExecution();
        taskExecution.setExecutionId(UUID.randomUUID());
        taskExecution.setWorkflowExecutionId(executionId);
        taskExecution.setTaskId(taskId);
        taskExecution.setStatus(TaskStatus.STARTING);
        taskExecution.setStartTime(LocalDateTime.now());
        
        taskExecutionRepository.save(taskExecution);
        
        // Check preconditions
        boolean preconditionsMet = evaluatePreconditions(task, completedTasks);
        
        if (!preconditionsMet && !Boolean.TRUE.equals(task.getForceExecution())) {
            // Skip this task
            taskExecution.setStatus(TaskStatus.SKIPPED);
            taskExecution.setEndTime(LocalDateTime.now());
            taskExecutionRepository.save(taskExecution);
            
            // Continue with next tasks
            for (String nextTaskId : dag.getNextTasks(taskId)) {
                executeTask(executionId, nextTaskId, dag, completedTasks);
            }
            return;
        }
        
        // Publish task start event
        eventBus.publish("task.start", Map.of(
            "executionId", executionId,
            "taskId", taskId
        ));
        
        // Update status to RUNNING
        taskExecution.setStatus(TaskStatus.RUNNING);
        taskExecutionRepository.save(taskExecution);
        
        try {
            // Execute the task
            Map<String, Object> result = taskExecutorService.executeTask(task, mapInputParameters(task, completedTasks));
            
            // Update task execution
            taskExecution.setStatus(TaskStatus.SUCCESS);
            taskExecution.setEndTime(LocalDateTime.now());
            taskExecution.setOutputParameters(result);
            taskExecutionRepository.save(taskExecution);
            
            // Add to completed tasks
            completedTasks.put(taskId, taskExecution);
            
            // Publish task completion event
            eventBus.publish("task.complete", Map.of(
                "executionId", executionId,
                "taskId", taskId,
                "status", TaskStatus.SUCCESS
            ));
            
            // Continue with next tasks
            for (String nextTaskId : dag.getNextTasks(taskId)) {
                executeTask(executionId, nextTaskId, dag, completedTasks);
            }
            
        } catch (Exception e) {
            handleTaskExecutionError(executionId, taskExecution, task, e);
            
            // Check if this failure should stop the workflow
            if (Boolean.TRUE.equals(task.getFailWorkflowOnError())) {
                throw new RuntimeException("Task failure is configured to fail workflow", e);
            }
            
            // Continue with next tasks despite the failure
            for (String nextTaskId : dag.getNextTasks(taskId)) {
                executeTask(executionId, nextTaskId, dag, completedTasks);
            }
        }
    }
    
    private boolean evaluatePreconditions(Task task, Map<String, TaskExecution> completedTasks) {
        // Implement precondition evaluation logic
        return true; // Simplified
    }
    
    private Map<String, Object> mapInputParameters(Task task, Map<String, TaskExecution> completedTasks) {
        // Logic to map input parameters based on task definitions and outputs from completed tasks
        return task.getInputParameters(); // Simplified
    }
    
    private void handleTaskExecutionError(UUID executionId, TaskExecution taskExecution, 
                                         Task task, Exception e) {
        // Update task execution
        taskExecution.setStatus(TaskStatus.FAILURE);
        taskExecution.setEndTime(LocalDateTime.now());
        taskExecutionRepository.save(taskExecution);
        
        // Publish task failure event
        eventBus.publish("task.complete", Map.of(
            "executionId", executionId,
            "taskId", task.getTaskId(),
            "status", TaskStatus.FAILURE,
            "error", e.getMessage()
        ));
    }
    
    private void handleWorkflowExecutionError(UUID executionId, Exception e) {
        WorkflowExecution execution = runningWorkflows.get(executionId);
        
        execution.setStatus(WorkflowStatus.FAILURE);
        execution.setEndTime(LocalDateTime.now());
        workflowExecutionRepository.save(execution);
        
        // Remove from running workflows
        runningWorkflows.remove(executionId);
        workflowDAGs.remove(executionId);
    }
    
    private void handleWorkflowInterruptEvent(Map<String, Object> eventData) {
        UUID executionId = (UUID) eventData.get("executionId");
        boolean waitForTaskCompletion = (boolean) eventData.getOrDefault("waitForTaskCompletion", false);
        
        stopWorkflow(executionId, waitForTaskCompletion);
    }
    
    @Transactional
    public void stopWorkflow(UUID executionId, boolean waitForTaskCompletion) {
        // Implementation to stop a workflow
    }
    
    @Transactional
    public WorkflowExecution restartWorkflow(UUID executionId, String taskId) {
        // Implementation to restart a workflow from a specific task
        return null;
    }
    
    @Cacheable(value = "workflows")
    public List<Workflow> getAllWorkflows() {
        List<Workflow> workflows = new ArrayList<>();
        workflowRepository.findAll().forEach(workflows::add);
        return workflows;
    }
    
    @Cacheable(value = "workflow", key = "#workflowId")
    public Workflow getWorkflow(String workflowId) {
        return workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
    }
    
    @Transactional
    @CacheEvict(value = "workflow", key = "#workflowId")
    public Workflow updateWorkflow(String workflowId, Map<String, Object> parameters) {
        Workflow workflow = getWorkflow(workflowId);
        
        // Update only non-protected parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            
            // Skip protected parameters
            if (workflow.getProtectedParameters().contains(paramName)) {
                continue;
            }
            
            // Skip unique identifiers
            if ("workflowId".equals(paramName)) {
                continue;
            }
            
            workflow.getParameters().put(paramName, entry.getValue());
        }
        
        return workflowRepository.save(workflow);
    }
    
    @Cacheable(value = "workflowExecutions", key = "'status-' + #status")
    public List<WorkflowExecution> getWorkflowExecutionsByStatus(WorkflowStatus status) {
        return workflowExecutionRepository.findByStatus(status);
    }
    
    @Cacheable(value = "workflowExecutions", key = "'workflow-' + #workflowId")
    public List<WorkflowExecution> getWorkflowExecutionsByWorkflowId(String workflowId) {
        return workflowExecutionRepository.findByWorkflowId(workflowId);
    }

    @Cacheable(value = "workflowExecutions", key = "'workflow-' + #executionId")
    public WorkflowExecution getWorkflowExecution(UUID executionId) {
        return workflowExecutionRepository.findByExecutionId(executionId).orElse(null);
    }

    @Scheduled(fixedDelay = 60000) // Run every minute
    public void scheduleWorkflows() {
        // Logic to check and schedule workflows based on their schedule
    }
}