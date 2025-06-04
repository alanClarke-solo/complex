package com.workflow.config;

import com.workflow.ulid.service.UlidServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Configuration for the gRPC server.
 */
@Slf4j
@Configuration
public class GrpcServerConfiguration {

    @Value("${grpc.server.port:9090}")
    private int grpcPort;

    /**
     * Configure and start the gRPC server.
     *
     * @param ulidService the ULID service implementation
     * @return the started gRPC server
     * @throws IOException if the server cannot be started
     */
    @Bean
    public Server grpcServer(UlidServiceImpl ulidService) throws IOException {
        Server server = ServerBuilder.forPort(grpcPort)
                .addService(ulidService)
                .build();

        server.start();
        log.info("gRPC Server started on port {}", grpcPort);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server");
            server.shutdown();
        }));

        return server;
    }
}