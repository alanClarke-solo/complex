package com.workflow.controller;

import com.workflow.exception.WorkflowException;
import com.workflow.service.WorkflowExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/workflow/execution")
public class WorkflowExecutionController {

    @Autowired
    private WorkflowExecutionService workflowExecutionService;

    /**
     * Start a workflow execution.
     *
     * @param workflowId the workflow ID
     * @return the execution response
     */
    @PostMapping("/start/{workflowId}")
    public ResponseEntity<Map<String, Object>> startWorkflow(@PathVariable String workflowId) {
        try {
            String executionId = workflowExecutionService.runWorkflow(workflowId);

            Map<String, Object> response = new HashMap<>();
            response.put("executionId", executionId);
            response.put("workflowId", workflowId);
            response.put("status", "STARTED");

            return ResponseEntity.ok(response);
        } catch (WorkflowException e) {
            log.error("Error starting workflow {}: {}", workflowId, e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error starting workflow {}: {}", workflowId, e.getMessage(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("error", "Internal server error: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get workflow execution status.
     *
     * @param executionId the execution ID
     * @return the execution status
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<WorkflowExecutionService.WorkflowExecutionStatus> getExecutionStatus(
            @PathVariable String executionId) {

        WorkflowExecutionService.WorkflowExecutionStatus status =
                workflowExecutionService.getWorkflowExecutionStatus(executionId);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(status);
    }

    /**
     * Clear workflow execution cache.
     *
     * @param executionId the execution ID
     * @return the response
     */
    @DeleteMapping("/cache/{executionId}")
    public ResponseEntity<Map<String, Object>> clearExecutionCache(@PathVariable String executionId) {
        workflowExecutionService.clearExecutionCache(executionId);

        Map<String, Object> response = new HashMap<>();
        response.put("executionId", executionId);
        response.put("status", "CACHE_CLEARED");

        return ResponseEntity.ok(response);
    }
}