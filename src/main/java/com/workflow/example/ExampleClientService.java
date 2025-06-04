package com.workflow.example;

import com.workflow.ulid.client.UlidClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Example service that uses the ULID client factory.
 */
@Service
public class ExampleClientService {
    private static final Logger logger = LoggerFactory.getLogger(ExampleClientService.class);

    @Value("${ulid.service.host:localhost}")
    private String ulidServiceHost;

    @Value("${ulid.service.port:8090}")
    private int ulidServicePort;

    private UlidClientFactory clientFactory;
    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        clientFactory = new UlidClientFactory(ulidServiceHost, ulidServicePort, "example-service");
        executorService = Executors.newFixedThreadPool(10);
        logger.info("Initialized ULID client factory connected to {}:{}", ulidServiceHost, ulidServicePort);
    }

    @PreDestroy
    public void destroy() {
        if (clientFactory != null) {
            clientFactory.close();
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Generate a single ULID.
     *
     * @return a new ULID
     */
    public String generateId() {
        return clientFactory.generateLocal();
    }

    /**
     * Generate a single monotonic ULID.
     *
     * @return a new monotonic ULID
     */
    public String generateMonotonicId() {
        return clientFactory.generateMonotonicLocal();
    }

    /**
     * Generate multiple ULIDs in parallel.
     *
     * @param count the number of ULIDs to generate
     * @return a list of generated ULIDs
     */
    public List<String> generateBulk(int count) {
        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            futures.add(CompletableFuture.supplyAsync(
                    clientFactory::generateLocal,
                    executorService
            ));
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    /**
     * Generate multiple monotonic ULIDs in parallel.
     *
     * @param count the number of ULIDs to generate
     * @return a list of generated monotonic ULIDs
     */
    public List<String> generateMonotonicBulk(int count) {
        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            futures.add(CompletableFuture.supplyAsync(
                    clientFactory::generateMonotonicLocal,
                    executorService
            ));
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }
}