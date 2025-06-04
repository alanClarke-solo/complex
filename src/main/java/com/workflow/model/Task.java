package com.workflow.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.function.Predicate;

@Data
@Table("TASKS")
public class Task {
    @Id
    private String taskId;
    private String workflowId;
    private String name;
    private String description;
    private Map<String, Object> inputParameters;
    private Map<String, Object> outputParameters;
    private List<Predicate<Object>> preconditions;
    private Boolean failWorkflowOnError;
    private Boolean forceExecution;
    private String schedule;
    private Boolean executeImmediately;
    private Boolean executeIfScheduleMissed;
    private TaskStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // Getters, setters, constructors
}