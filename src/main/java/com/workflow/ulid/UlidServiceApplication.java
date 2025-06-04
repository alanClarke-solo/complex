package com.workflow.ulid;

import com.workflow.ulid.service.UlidServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@SpringBootApplication
public class UlidServiceApplication {
    private static final Logger logger = LoggerFactory.getLogger(UlidServiceApplication.class);

    @Value("${grpc.server.port:8090}")
    private int grpcPort;

    public static void main(String[] args) {
        SpringApplication.run(UlidServiceApplication.class, args);
    }

    @Bean
    public Server grpcServer(UlidServiceImpl ulidService) throws IOException {
        Server server = ServerBuilder.forPort(grpcPort)
                .addService(ulidService)
                .build();

        server.start();
        logger.info("gRPC Server started on port {}", grpcPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server");
            server.shutdown();
        }));

        return server;
    }
}