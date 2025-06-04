
package com.workflow.ulid.client;

import com.workflow.ulid.ULIDFactory;
import com.workflow.ulid.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Client factory for generating ULIDs with server coordination.
 * <p>
 * This client connects to the ULID service to ensure distributed
 * uniqueness, but can also generate ULIDs locally when in a valid
 * time window with an assigned node ID.
 */
public class UlidClientFactory implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(UlidClientFactory.class);

    private final String clientId;
    private final ManagedChannel channel;
    private final UlidServiceGrpc.UlidServiceBlockingStub blockingStub;

    private final ReentrantLock batchLock = new ReentrantLock();
    private final AtomicBoolean reservationValid = new AtomicBoolean(false);

    private ULIDFactory localFactory;
    private long reservationEndTime;
    private long waitTimeMs;

    /**
     * Creates a new ULID client factory.
     *
     * @param host the host of the ULID service
     * @param port the port of the ULID service
     */
    public UlidClientFactory(String host, int port) {
        this(host, port, UUID.randomUUID().toString());
    }

    /**
     * Creates a new ULID client factory with a specific client ID.
     *
     * @param host     the host of the ULID service
     * @param port     the port of the ULID service
     * @param clientId the client identifier
     */
    public UlidClientFactory(String host, int port, String clientId) {
        this.clientId = clientId;
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = UlidServiceGrpc.newBlockingStub(channel);

        // Initialize with a batch reservation
        reserveBatch(1000);
    }

    /**
     * Generates a ULID using the service.
     *
     * @return a new ULID
     */
    public String generate() {
        UlidResponse response = blockingStub.generateUlid(
                UlidRequest.newBuilder().setClientId(clientId).build());
        return response.getUlid();
    }

    /**
     * Generates a monotonic ULID using the service.
     *
     * @return a new monotonic ULID
     */
    public String generateMonotonic() {
        UlidResponse response = blockingStub.generateMonotonicUlid(
                UlidRequest.newBuilder().setClientId(clientId).build());
        return response.getUlid();
    }

    /**
     * Generates a ULID locally if within a valid reservation window,
     * otherwise calls the service.
     *
     * @return a new ULID
     */
    public String generateLocal() {
        if (canGenerateLocally()) {
            return localFactory.generate();
        } else {
            // No valid reservation, call the service
            return generate();
        }
    }

    /**
     * Generates a monotonic ULID locally if within a valid reservation window,
     * otherwise calls the service.
     *
     * @return a new monotonic ULID
     */
    public String generateMonotonicLocal() {
        if (canGenerateLocally()) {
            return localFactory.generateMonotonic();
        } else {
            // No valid reservation, call the service
            return generateMonotonic();
        }
    }

    /**
     * Checks if local generation is possible and renews the reservation if needed.
     *
     * @return true if local generation is possible
     */
    private boolean canGenerateLocally() {
        if (reservationValid.get() && System.currentTimeMillis() < reservationEndTime) {
            return true;
        }

        // Need to reserve a new batch
        if (batchLock.tryLock()) {
            try {
                // Check again in case another thread reserved while we were waiting
                if (reservationValid.get() && System.currentTimeMillis() < reservationEndTime) {
                    return true;
                }

                // Reserve a new batch
                return reserveBatch(1000);
            } finally {
                batchLock.unlock();
            }
        }

        // Couldn't get lock, another thread is likely reserving
        return false;
    }

    /**
     * Reserves a batch of ULIDs from the service.
     *
     * @param batchSize the number of ULIDs to reserve
     * @return true if reservation was successful
     */
    private boolean reserveBatch(int batchSize) {
        try {
            BatchResponse response = blockingStub.reserveBatch(
                    BatchRequest.newBuilder()
                            .setClientId(clientId)
                            .setBatchSize(batchSize)
                            .build());

            int nodeId = response.getNodeId();
            reservationEndTime = response.getTimestampEnd();
            waitTimeMs = response.getWaitTimeMs();

            // Create or update local factory with assigned node ID
            if (localFactory == null || localFactory.getNodeId() != nodeId) {
                localFactory = new ULIDFactory(nodeId);
            }

            reservationValid.set(true);
            logger.info("Reserved batch: nodeId={}, validUntil={}, waitTime={}ms",
                    nodeId, reservationEndTime, waitTimeMs);

            return true;
        } catch (Exception e) {
            logger.error("Failed to reserve batch: {}", e.getMessage(), e);
            reservationValid.set(false);
            return false;
        }
    }

    @Override
    public void close() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Channel shutdown interrupted", e);
        }
    }
}