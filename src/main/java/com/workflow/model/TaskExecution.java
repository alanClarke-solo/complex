package com.workflow.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Table("TASK_EXECUTIONS")
public class TaskExecution {
    @Id
    private UUID executionId;
    private UUID workflowExecutionId;
    private String taskId;
    private TaskStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, Object> inputParameters;
    private Map<String, Object> outputParameters;
    
    // Getters, setters, constructors
}