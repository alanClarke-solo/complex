package com.workflow.service;

import com.workflow.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TaskExecutorService {
    private final RestTemplate restTemplate;
    private final RemoteShellExecutor remoteShellExecutor;
    
    public Map<String, Object> executeTask(Task task, Map<String, Object> inputParameters) {
        String taskType = (String) task.getInputParameters().get("type");
        
        switch (taskType) {
            case "rest":
                return executeRestTask(task, inputParameters);
            case "shell":
                return executeShellTask(task, inputParameters);
            default:
                throw new IllegalArgumentException("Unsupported task type: " + taskType);
        }
    }
    
    private Map<String, Object> executeRestTask(Task task, Map<String, Object> inputParameters) {
        String url = (String) inputParameters.get("url");
        String method = (String) inputParameters.get("method");
        Object body = inputParameters.get("body");
        boolean async = Boolean.parseBoolean((String) inputParameters.getOrDefault("async", "false"));
        
        if (async) {
            CompletableFuture.runAsync(() -> {
                // Execute REST call asynchronously
                executeRestCall(url, method, body);
            });
            
            // Return immediately
            return Map.of("status", "async_submitted");
        } else {
            // Execute synchronously and return result
            return executeRestCall(url, method, body);
        }
    }
    
    private Map<String, Object> executeRestCall(String url, String method, Object body) {
        // Implementation for different HTTP methods
        return Map.of("response", "sample_response");
    }
    
    private Map<String, Object> executeShellTask(Task task, Map<String, Object> inputParameters) {
        String host = (String) inputParameters.get("host");
        String command = (String) inputParameters.get("command");
        
        String output = remoteShellExecutor.executeCommand(host, command);
        return Map.of("output", output);
    }
}