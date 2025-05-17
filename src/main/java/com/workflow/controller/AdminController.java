package com.workflow.controller;

import com.workflow.config.WorkflowConfiguration;
import com.workflow.model.Task;
import com.workflow.model.Workflow;
import com.workflow.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    
    @PutMapping("/config/cache")
    public ResponseEntity<Void> updateCacheConfig(@RequestBody Map<String, Object> config) {
        adminService.updateCacheConfiguration(config);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/config/execution")
    public ResponseEntity<Void> updateExecutionConfig(@RequestBody Map<String, Object> config) {
        adminService.updateExecutionConfiguration(config);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/config/scheduler")
    public ResponseEntity<Void> updateSchedulerConfig(@RequestBody Map<String, Object> config) {
        adminService.updateSchedulerConfiguration(config);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/workflows/{workflowId}")
    public ResponseEntity<Workflow> updateWorkflowProperties(
            @PathVariable String workflowId,
            @RequestBody Workflow workflow) {
        return ResponseEntity.ok(adminService.updateWorkflowProperties(workflowId, workflow));
    }
    
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<Task> updateTaskProperties(
            @PathVariable String taskId,
            @RequestBody Task task) {
        return ResponseEntity.ok(adminService.updateTaskProperties(taskId, task));
    }
    
    @PostMapping("/reload-configurations")
    public ResponseEntity<Void> reloadConfigurations() {
        adminService.reloadConfigurations();
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/system-status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        return ResponseEntity.ok(adminService.getSystemStatus());
    }
}