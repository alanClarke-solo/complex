package com.workflow.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Table("WORKFLOWS")
public class Workflow {
    @Id
    private String workflowId;
    private String name;
    private String description;
    private Map<String, Object> parameters;
    private List<String> protectedParameters;
    private String schedule;
    private WorkflowStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    // Getters, setters, constructors
}