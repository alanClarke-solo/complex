package com.workflow.service.task;

import com.workflow.model.Task;
import com.workflow.service.TaskExecutionService.TaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Task handler for HTTP requests.
 */
@Slf4j
@Component
public class HttpTaskHandler implements TaskHandler {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Execute an HTTP task.
     *
     * @param task the task to execute
     * @return the task result
     */
    @Override
    public Map<String, Object> execute(Task task) throws Exception {
        Map<String, Object> inputParams = task.getInputParameters();

        if (inputParams == null) {
            throw new IllegalArgumentException("Task input parameters cannot be null");
        }

        // Get required parameters
        String url = getRequiredParam(inputParams, "url", String.class);
        String method = getParam(inputParams, "method", "GET", String.class);

        // Get optional parameters
        Map<String, String> headers = getParam(inputParams, "headers", new HashMap<>(), Map.class);
        Object body = getParam(inputParams, "body", null, Object.class);

        log.info("Executing HTTP task: {} {} ({})", method, url, task.getTaskId());

        // Create headers
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::add);

        // Create entity
        HttpEntity<?> entity = new HttpEntity<>(body, httpHeaders);

        // Execute request
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(method),
                entity,
                String.class
        );

        // Process response
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", response.getStatusCodeValue());
        result.put("body", response.getBody());
        result.put("headers", response.getHeaders());

        return result;
    }

    /**
     * Get a required parameter from task input.
     *
     * @param params the input parameters
     * @param name the parameter name
     * @param type the parameter type
     * @return the parameter value
     * @throws IllegalArgumentException if the parameter is missing
     */
    @SuppressWarnings("unchecked")
    private <T> T getRequiredParam(Map<String, Object> params, String name, Class<T> type) {
        Object value = params.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter missing: " + name);
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Parameter " + name + " is of wrong type, expected: " + type.getSimpleName());
        }

        return (T) value;
    }

    /**
     * Get an optional parameter from task input.
     *
     * @param params the input parameters
     * @param name the parameter name
     * @param defaultValue the default value
     * @param type the parameter type
     * @return the parameter value, or the default if not present
     */
    @SuppressWarnings("unchecked")
    private <T> T getParam(Map<String, Object> params, String name, T defaultValue, Class<?> type) {
        Object value = params.get(name);
        if (value == null) {
            return defaultValue;
        }

        if (!type.isInstance(value)) {
            log.warn("Parameter {} is of wrong type, expected: {}, got: {}",
                    name, type.getSimpleName(), value.getClass().getSimpleName());
            return defaultValue;
        }

        return (T) value;
    }
}