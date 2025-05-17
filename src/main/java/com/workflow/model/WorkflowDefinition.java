package com.workflow.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Container class for workflow definition JSON structure
 */
@Setter
@Getter
public class WorkflowDefinition {
    private Workflow workflow;
    private List<Task> tasks;

}
