package com.workflow.config;

import com.workflow.exception.CustomException;
import com.workflow.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Helper class to standardize error handling for reactive Mono operations
 */
@Component
@Slf4j
public class ReactiveMonoErrorHandler {

    /**
     * Handle errors in a standardized way for any Mono operation
     * 
     * @param <T> Type of the Mono
     * @param serviceOperation Description of service operation for logging
     * @return Function to handle errors
     */
    public <T> Function<Throwable, Mono<T>> handleError(String serviceOperation) {
        return throwable -> {
            log.error("Error during {}: {}", serviceOperation, throwable.getMessage(), throwable);
            
            if (throwable instanceof CustomException) {
                return Mono.error(throwable);
            } else if (throwable instanceof TimeoutException) {
                return Mono.error(new ExternalServiceException(
                        "Operation timed out: " + serviceOperation, 
                        HttpStatus.GATEWAY_TIMEOUT, 
                        throwable));
            } else if (throwable instanceof WebClientResponseException) {
                WebClientResponseException wcException = (WebClientResponseException) throwable;
                return Mono.error(new ExternalServiceException(
                        "External service error during " + serviceOperation + ": " + wcException.getMessage(),
                        HttpStatus.valueOf(wcException.getStatusCode().value()),
                        throwable));
            } else {
                return Mono.error(new ExternalServiceException(
                        "Unexpected error during " + serviceOperation + ": " + throwable.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        throwable));
            }
        };
    }
}
