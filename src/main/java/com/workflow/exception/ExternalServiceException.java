package com.workflow.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends CustomException {
    private static final String ERROR_CODE = "EXTERNAL_SERVICE_ERROR";

    public ExternalServiceException(String message) {
        super(message, ERROR_CODE, HttpStatus.BAD_GATEWAY);
    }

    public ExternalServiceException(String message, HttpStatus status) {
        super(message, ERROR_CODE, status);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, ERROR_CODE, HttpStatus.BAD_GATEWAY, cause);
    }

    public ExternalServiceException(String message, HttpStatus status, Throwable cause) {
        super(message, ERROR_CODE, status, cause);
    }
}
