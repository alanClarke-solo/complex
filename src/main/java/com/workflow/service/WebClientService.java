package com.workflow.service;

import com.workflow.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class WebClientService {

    private final WebClient webClient;

    /**
     * Make a GET request to an external service
     * This method includes error handling and retry logic
     */
    public <T> Mono<T> getFromExternalService(String url, Class<T> responseType) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(TimeoutException.class, ex -> 
                    new ExternalServiceException("Request to external service timed out", HttpStatus.GATEWAY_TIMEOUT, ex))
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error("Error response from GET from external service: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    return new ExternalServiceException("Error from external service: " + ex.getMessage(), 
                            HttpStatus.valueOf(ex.getStatusCode().value()), ex);
                })
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("Failed to get response after max retries", retrySignal.failure());
                            return new ExternalServiceException(
                                    "External service unavailable after retries", 
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    retrySignal.failure());
                        }));
    }

    /**
     * Make a POST request to an external service
     * This method includes error handling and retry logic
     */
    public <T, R> Mono<R> postToExternalService(String url, T requestBody, Class<R> responseType) {
        return webClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(responseType)
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(TimeoutException.class, ex -> 
                    new ExternalServiceException("Request to external service timed out", HttpStatus.GATEWAY_TIMEOUT, ex))
                .onErrorMap(WebClientResponseException.class, ex -> {
                    log.error("Error response from POST to external service: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    return new ExternalServiceException("Error from external service: " + ex.getMessage(), 
                            HttpStatus.valueOf(ex.getStatusCode().value()), ex);
                })
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500))
                        .filter(throwable -> !(throwable instanceof WebClientResponseException.BadRequest))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("Failed to get response after max retries", retrySignal.failure());
                            return new ExternalServiceException(
                                    "External service unavailable after retries", 
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    retrySignal.failure());
                        }));
    }
}
