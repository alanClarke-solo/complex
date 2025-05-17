package com.workflow.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("WORKFLOW_EXECUTIONS")
public class WorkflowExecution {
    @Id
    private UUID executionId;
    private String workflowId;
    private WorkflowStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // Getters, setters, constructors
}