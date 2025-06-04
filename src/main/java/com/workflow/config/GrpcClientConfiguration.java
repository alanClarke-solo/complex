package com.workflow.config;

import com.workflow.ulid.client.UlidClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for gRPC clients.
 */
@Slf4j
@Configuration
public class GrpcClientConfiguration {

    @Value("${grpc.ulid.host:localhost}")
    private String ulidServiceHost;

    @Value("${grpc.ulid.port:9090}")
    private int ulidServicePort;

    @Value("${application.name:workflow-service}")
    private String applicationName;

    /**
     * Creates a ULID client factory.
     *
     * @return the ULID client factory
     */
    @Bean
    @Primary
    public UlidClientFactory ulidClientFactory() {
        String clientId = applicationName + "-" + System.currentTimeMillis();
        log.info("Creating ULID client with ID {} connecting to {}:{}",
                clientId, ulidServiceHost, ulidServicePort);

        return new UlidClientFactory(ulidServiceHost, ulidServicePort, clientId);
    }
}