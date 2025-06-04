package com.workflow.service;

import com.workflow.model.Task;
import com.workflow.model.TaskStatus;
import com.workflow.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@CacheConfig(cacheNames = "tasks")
public class TaskExecutionService {

    @Autowired
    private TaskRepository taskRepository;

    // Registry of task handlers
    private final Map<String, TaskHandler> taskHandlers = new ConcurrentHashMap<>();

    /**
     * Execute a task.
     *
     * @param task the task to execute
     * @return true if the task executed successfully
     */
    public boolean executeTask(Task task) {
        log.info("Executing task: {} ({})", task.getName(), task.getTaskId());

        try {
            // Find appropriate task handler
            TaskHandler handler = findTaskHandler(task);

            if (handler == null) {
                log.error("No handler found for task type: {}", task.getName());
                return false;
            }

            // Execute the task
            Map<String, Object> result = handler.execute(task);

            // Store the task result
            if (result != null) {
                task.setOutputParameters(result);
            } else {
                task.setOutputParameters(new HashMap<>());
            }

            return true;
        } catch (Exception e) {
            log.error("Error executing task: {} ({}): {}",
                    task.getName(), task.getTaskId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Find a task handler for the given task.
     *
     * @param task the task
     * @return the task handler, or null if not found
     */
    private TaskHandler findTaskHandler(Task task) {
        // Simple implementation - lookup by task name
        return taskHandlers.get(task.getName());
    }

    /**
     * Register a task handler.
     *
     * @param taskType the task type
     * @param handler the task handler
     */
    public void registerTaskHandler(String taskType, TaskHandler handler) {
        taskHandlers.put(taskType, handler);
        log.info("Registered task handler for task type: {}", taskType);
    }

    /**
     * Get the status of a task.
     *
     * @param taskId the task ID
     * @return the task status
     */
    @Cacheable(key = "#taskId", unless = "#result == null")
    public TaskStatus getTaskStatus(String taskId) {
        return taskRepository.findById(taskId)
                .map(Task::getStatus)
                .orElse(null);
    }

    /**
     * Find tasks by workflow ID.
     *
     * @param workflowId the workflow ID
     * @return the list of tasks
     */
    @Cacheable(key = "'workflow_' + #workflowId", unless = "#result.isEmpty()")
    public List<Task> findTasksByWorkflowId(String workflowId) {
        return taskRepository.findByWorkflowId(workflowId);
    }

    /**
     * Clear the task cache for a workflow.
     *
     * @param workflowId the workflow ID
     */
    @CacheEvict(key = "'workflow_' + #workflowId")
    public void clearWorkflowTaskCache(String workflowId) {
        log.info("Cleared task cache for workflow: {}", workflowId);
    }

    /**
     * Clear the task status cache.
     *
     * @param taskId the task ID
     */
    @CacheEvict(key = "#taskId")
    public void clearTaskStatusCache(String taskId) {
        log.info("Cleared task status cache: {}", taskId);
    }

    /**
     * Interface for task handlers.
     */
    public interface TaskHandler {
        /**
         * Execute a task.
         *
         * @param task the task to execute
         * @return the task result
         */
        Map<String, Object> execute(Task task) throws Exception;
    }
}