package com.workflow.util;

import com.workflow.exception.CustomException;
import com.workflow.exception.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Utility class to handle exceptions in asynchronous operations
 */
@Slf4j
public class AsyncExceptionWrapper {

    /**
     * Wraps a CompletableFuture supplier to handle exceptions properly
     * 
     * @param <T> The type of the CompletableFuture result
     * @param operation Description of the operation
     * @param supplier The supplier that returns a CompletableFuture
     * @return The result of the CompletableFuture
     */
    public static <T> T handleAsync(String operation, Supplier<CompletableFuture<T>> supplier) {
        try {
            return supplier.get().join();
        } catch (CompletionException e) {
            return handleAsyncException(operation, e.getCause() != null ? e.getCause() : e);
        }
    }

    /**
     * Handle exceptions from asynchronous operations
     */
    private static <T> T handleAsyncException(String operation, Throwable e) {
        log.error("Error in async operation {}: {}", operation, e.getMessage(), e);
        
        if (e instanceof CustomException) {
            throw (CustomException) e;
        } else if (e instanceof ExecutionException && e.getCause() instanceof CustomException) {
            throw (CustomException) e.getCause();
        } else if (e instanceof WebClientResponseException) {
            WebClientResponseException wcException = (WebClientResponseException) e;
            throw new ExternalServiceException(
                    "External service error during " + operation + ": " + wcException.getMessage(),
                    HttpStatus.valueOf(wcException.getStatusCode().value()),
                    e);
        } else {
            throw new ExternalServiceException(
                    "Error in async operation " + operation + ": " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }
}
