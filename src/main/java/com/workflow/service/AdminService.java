package com.workflow.service;

import com.workflow.config.WorkflowConfiguration;
import com.workflow.model.Task;
import com.workflow.model.Workflow;
import com.workflow.repository.TaskRepository;
import com.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final WorkflowRepository workflowRepository;
    private final TaskRepository taskRepository;
    private final CacheManager cacheManager;
    private final TaskScheduler taskScheduler;
    private final WorkflowConfiguration workflowConfiguration;
    
    @Transactional
    public void updateCacheConfiguration(Map<String, Object> config) {
        if (cacheManager instanceof RedisCacheManager) {
            // Update Redis cache configuration
            // This is simplified and would need actual implementation to dynamically 
            // update Redis cache properties
            
            // Update the configuration properties
            workflowConfiguration.getCache().putAll(config);
            
            // Clear all caches to apply new settings
            cacheManager.getCacheNames().forEach(cacheName -> 
                cacheManager.getCache(cacheName).clear());
        }
    }
    
    public void updateExecutionConfiguration(Map<String, Object> config) {
        // Update execution-related configuration
        workflowConfiguration.getExecution().putAll(config);
    }
    
    public void updateSchedulerConfiguration(Map<String, Object> config) {
        // Update scheduler-related configuration
        workflowConfiguration.getScheduler().putAll(config);
    }
    
    @Transactional
    public Workflow updateWorkflowProperties(String workflowId, Workflow updatedWorkflow) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));
        
        // Update non-identifier properties
        if (updatedWorkflow.getName() != null) {
            workflow.setName(updatedWorkflow.getName());
        }
        
        if (updatedWorkflow.getDescription() != null) {
            workflow.setDescription(updatedWorkflow.getDescription());
        }
        
        if (updatedWorkflow.getParameters() != null) {
            // Only update non-protected parameters
            updatedWorkflow.getParameters().forEach((key, value) -> {
                if (!workflow.getProtectedParameters().contains(key)) {
                    workflow.getParameters().put(key, value);
                }
            });
        }
        
        if (updatedWorkflow.getSchedule() != null) {
            workflow.setSchedule(updatedWorkflow.getSchedule());
        }
        
        return workflowRepository.save(workflow);
    }
    
    @Transactional
    public Task updateTaskProperties(String taskId, Task updatedTask) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        // Update non-identifier properties
        if (updatedTask.getName() != null) {
            task.setName(updatedTask.getName());
        }
        
        if (updatedTask.getDescription() != null) {
            task.setDescription(updatedTask.getDescription());
        }
        
        if (updatedTask.getInputParameters() != null) {
            task.setInputParameters(updatedTask.getInputParameters());
        }
        
        if (updatedTask.getPreconditions() != null) {
            task.setPreconditions(updatedTask.getPreconditions());
        }
        
        if (updatedTask.getFailWorkflowOnError() != null) {
            task.setFailWorkflowOnError(updatedTask.getFailWorkflowOnError());
        }
        
        if (updatedTask.getForceExecution() != null) {
            task.setForceExecution(updatedTask.getForceExecution());
        }
        
        if (updatedTask.getSchedule() != null) {
            task.setSchedule(updatedTask.getSchedule());
        }
        
        if (updatedTask.getExecuteImmediately() != null) {
            task.setExecuteImmediately(updatedTask.getExecuteImmediately());
        }
        
        if (updatedTask.getExecuteIfScheduleMissed() != null) {
            task.setExecuteIfScheduleMissed(updatedTask.getExecuteIfScheduleMissed());
        }
        
        return taskRepository.save(task);
    }
    
    public void reloadConfigurations() {
        // Reload configurations from the file system
        // This would involve re-reading configuration files and updating the in-memory state
    }
    
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Add system metrics
        status.put("activeWorkflows", getActiveWorkflowCount());
        status.put("completedWorkflows", getCompletedWorkflowCount());
        status.put("failedWorkflows", getFailedWorkflowCount());
        status.put("cacheStatus", getCacheStatus());
        status.put("databaseStatus", getDatabaseStatus());
        
        return status;
    }
    
    private long getActiveWorkflowCount() {
        // Implementation to get count of active workflows
        return 0; // Placeholder
    }
    
    private long getCompletedWorkflowCount() {
        // Implementation to get count of completed workflows
        return 0; // Placeholder
    }
    
    private long getFailedWorkflowCount() {
        // Implementation to get count of failed workflows
        return 0; // Placeholder
    }
    
    private Map<String, Object> getCacheStatus() {
        // Implementation to get cache status
        return Map.of("hitRatio", 0.85, "size", 1024); // Placeholder
    }
    
    private Map<String, Object> getDatabaseStatus() {
        // Implementation to get database status
        return Map.of("connectionPool", "healthy", "activeConnections", 5); // Placeholder
    }
}