
package com.workflow.ulid.service;

import com.workflow.ulid.grpc.*;
import com.workflow.util.ULIDGenerator;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of the ULID service gRPC API.
 */
@Slf4j
@Service
public class UlidServiceImpl extends UlidServiceGrpc.UlidServiceImplBase {

    @Autowired
    private ULIDGenerator ulidGenerator;

    // Map to track monotonic ULID generation per client
    private final Map<String, String> lastUlidByClient = new ConcurrentHashMap<>();

    // Map to track client node IDs
    private final Map<String, Integer> clientNodeIds = new ConcurrentHashMap<>();

    // Node ID counter
    private final AtomicInteger nodeIdCounter = new AtomicInteger(0);

    // Maximum node ID value (10 bits = 1024 values)
    private static final int MAX_NODE_ID = 1023;

    /**
     * Generate a standard ULID.
     *
     * @param request the ULID request
     * @param responseObserver the response observer
     */
    @Override
    public void generateUlid(UlidRequest request, StreamObserver<UlidResponse> responseObserver) {
        String clientId = request.getClientId();
        log.debug("Generating ULID for client: {}", clientId);

        long timestamp = Instant.now().toEpochMilli();
        String ulid = ulidGenerator.generate();

        UlidResponse response = UlidResponse.newBuilder()
                .setUlid(ulid)
                .setTimestamp(timestamp)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Generate a monotonic ULID.
     *
     * @param request the ULID request
     * @param responseObserver the response observer
     */
    @Override
    public void generateMonotonicUlid(UlidRequest request, StreamObserver<UlidResponse> responseObserver) {
        String clientId = request.getClientId();
        log.debug("Generating monotonic ULID for client: {}", clientId);

        long timestamp = Instant.now().toEpochMilli();

        // Get the last ULID for this client, if any
        String lastUlid = lastUlidByClient.get(clientId);

        // Generate a new monotonic ULID
        String ulid = generateMonotonicUlid(lastUlid);

        // Store the new ULID as the last one for this client
        lastUlidByClient.put(clientId, ulid);

        UlidResponse response = UlidResponse.newBuilder()
                .setUlid(ulid)
                .setTimestamp(timestamp)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Reserve a batch of ULIDs for client-side generation.
     *
     * @param request the batch request
     * @param responseObserver the response observer
     */
    @Override
    public void reserveBatch(BatchRequest request, StreamObserver<BatchResponse> responseObserver) {
        String clientId = request.getClientId();
        int batchSize = request.getBatchSize();

        log.debug("Reserving batch of {} ULIDs for client: {}", batchSize, clientId);

        // Get or assign a node ID for this client
        int nodeId = clientNodeIds.computeIfAbsent(clientId, k ->
                nodeIdCounter.getAndUpdate(n -> n >= MAX_NODE_ID ? 0 : n + 1));

        // Calculate time window for this batch
        long now = Instant.now().toEpochMilli();
        long endTime = now + 10000; // Reserve 10 seconds

        // Calculate wait time (50% of window size)
        long waitTime = 5000;

        BatchResponse response = BatchResponse.newBuilder()
                .setNodeId(nodeId)
                .setTimestampStart(now)
                .setTimestampEnd(endTime)
                .setWaitTimeMs(waitTime)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Generate a monotonic ULID based on the previous ULID.
     *
     * @param lastUlid the previous ULID, or null if none
     * @return a new monotonic ULID
     */
    private String generateMonotonicUlid(String lastUlid) {
        // For now, just generate a new ULID
        // In a real implementation, this would ensure the new ULID is
        // lexicographically greater than the last one
        return ulidGenerator.generate();
    }
}