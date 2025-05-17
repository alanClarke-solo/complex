package com.workflow.controller;

import com.workflow.engine.WorkflowManager;
import com.workflow.model.Workflow;
import com.workflow.model.WorkflowExecution;
import com.workflow.model.WorkflowStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowManagementController {
    private final WorkflowManager workflowManager;
    
    @GetMapping
    public List<Workflow> getAllWorkflows() {
        return workflowManager.getAllWorkflows();
    }
    
    @GetMapping("/{workflowId}")
    public Workflow getWorkflow(@PathVariable String workflowId) {
        return workflowManager.getWorkflow(workflowId);
    }
    
    @PutMapping("/{workflowId}")
    public Workflow updateWorkflow(
            @PathVariable String workflowId,
            @RequestBody Map<String, Object> parameters) {
        return workflowManager.updateWorkflow(workflowId, parameters);
    }
    
    @GetMapping("/executions")
    public List<WorkflowExecution> getWorkflowExecutions(
            @RequestParam(required = false) WorkflowStatus status) {
        if (status != null) {
            return workflowManager.getWorkflowExecutionsByStatus(status);
        }
        // Logic to return all executions
        return List.of(); // Placeholder
    }
    
    @GetMapping("/executions/{workflowId}")
    public List<WorkflowExecution> getWorkflowExecutionsByWorkflowId(
            @PathVariable String workflowId) {
        return workflowManager.getWorkflowExecutionsByWorkflowId(workflowId);
    }
    
    @PostMapping("/{workflowId}/start")
    public ResponseEntity<WorkflowExecution> startWorkflow(
            @PathVariable String workflowId) {
        WorkflowExecution execution = workflowManager.startWorkflow(workflowId);
        return ResponseEntity.ok(execution);
    }
    
    @PostMapping("/executions/{executionId}/stop")
    public ResponseEntity<Void> stopWorkflow(
            @PathVariable UUID executionId,
            @RequestParam(defaultValue = "false") boolean waitForTaskCompletion) {
        workflowManager.stopWorkflow(executionId, waitForTaskCompletion);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/executions/{executionId}/restart")
    public ResponseEntity<WorkflowExecution> restartWorkflow(
            @PathVariable UUID executionId,
            @RequestParam(required = false) String taskId) {
        WorkflowExecution execution = workflowManager.restartWorkflow(executionId, taskId);
        return ResponseEntity.ok(execution);
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<WorkflowExecution> getWorkflowExecution(
            @PathVariable UUID executionId) {
        // Get execution details by ID
        return ResponseEntity.ok(workflowManager.getWorkflowExecution(executionId));
    }
}