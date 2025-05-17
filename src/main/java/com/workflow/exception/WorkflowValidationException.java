package com.workflow.exception;

public class WorkflowValidationException extends RuntimeException {
    public WorkflowValidationException(String message) {
        super(message);
    }
}
