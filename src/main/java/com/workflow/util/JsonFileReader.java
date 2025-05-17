package com.workflow.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.workflow.exception.WorkflowValidationException;
import com.workflow.model.Task;
import com.workflow.model.Workflow;
import com.workflow.model.WorkflowDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility class for reading workflow definitions from JSON files
 * and converting them to domain objects.
 */
@Component
@Slf4j
public class JsonFileReader {

    private final ObjectMapper objectMapper;

    public JsonFileReader() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Reads a workflow definition from a JSON file
     *
     * @param filePath the path to the JSON file
     * @return the workflow definition
     * @throws IOException if there's an error reading the file
     * @throws WorkflowValidationException if the file contains invalid workflow definition
     */
    public WorkflowDefinition readWorkflowDefinition(Path filePath) throws IOException {
        log.debug("Reading workflow definition from file: {}", filePath);

        String content = Files.readString(filePath);

        try {
            WorkflowDefinition definition = objectMapper.readValue(content, WorkflowDefinition.class);

            // Validate the workflow definition
            validateWorkflowDefinition(definition);

            return definition;
        } catch (Exception e) {
            log.error("Failed to parse workflow definition from file: {}", filePath, e);
            throw new WorkflowValidationException("Invalid workflow definition in file: " + filePath +
                    " - " + e.getMessage());
        }
    }

    /**
     * Validates a workflow definition
     *
     * @param definition the workflow definition to validate
     * @throws WorkflowValidationException if the definition is invalid
     */
    private void validateWorkflowDefinition(WorkflowDefinition definition) {
        // Check for required fields
        if (definition.getWorkflow() == null) {
            throw new WorkflowValidationException("Workflow definition must include a workflow section");
        }

        Workflow workflow = definition.getWorkflow();

        if (workflow.getWorkflowId() == null || workflow.getWorkflowId().isBlank()) {
            throw new WorkflowValidationException("Workflow must have a non-empty workflowId");
        }

        if (workflow.getName() == null || workflow.getName().isBlank()) {
            throw new WorkflowValidationException("Workflow must have a non-empty name");
        }

        // Validate tasks
        List<Task> tasks = definition.getTasks();
        if (tasks == null || tasks.isEmpty()) {
            throw new WorkflowValidationException("Workflow must have at least one task");
        }

        // Check that all tasks have the same workflowId as the workflow
        for (Task task : tasks) {
            if (task.getTaskId() == null || task.getTaskId().isBlank()) {
                throw new WorkflowValidationException("Task must have a non-empty taskId");
            }

            if (task.getName() == null || task.getName().isBlank()) {
                throw new WorkflowValidationException("Task must have a non-empty name");
            }

            if (!workflow.getWorkflowId().equals(task.getWorkflowId())) {
                throw new WorkflowValidationException(
                        "Task workflowId (" + task.getWorkflowId() +
                                ") does not match workflow workflowId (" + workflow.getWorkflowId() + ")");
            }
        }

        // Check uniqueness of task IDs
        long uniqueTaskIds = tasks.stream()
                .map(Task::getTaskId)
                .distinct()
                .count();

        if (uniqueTaskIds != tasks.size()) {
            throw new WorkflowValidationException("Task IDs must be unique within a workflow");
        }
    }

}