
package com.workflow.ulid;

import com.workflow.util.ULID;

import java.security.SecureRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Factory for generating ULIDs with distributed service safety.
 * <p>
 * This factory provides thread-safe and node-aware ULID generation
 * to avoid collisions in distributed environments.
 */
public class ULIDFactory {
    private static final ReentrantLock MONOTONIC_LOCK = new ReentrantLock();
    private static final int NODE_ID_BITS = 10; // 10 bits for node ID (max 1024 nodes)
    private static final int MAX_NODE_ID = (1 << NODE_ID_BITS) - 1;
    private static final char[] ENCODING_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
            'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X',
            'Y', 'Z'
    };

    private final int nodeId;
    private long lastTimestamp = -1L;
    private byte[] lastRandomness = new byte[10]; // 80 bits = 10 bytes
    private final SecureRandom secureRandom;

    /**
     * Creates a new ULIDFactory with the specified node ID.
     *
     * @param nodeId the node ID (0-1023)
     * @throws IllegalArgumentException if the node ID is invalid
     */
    public ULIDFactory(int nodeId) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException("Node ID must be between 0 and " + MAX_NODE_ID);
        }
        this.nodeId = nodeId;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generates a standard ULID.
     *
     * @return a new ULID as a String
     */
    public String generate() {
        return ULID.generate();
    }

    /**
     * Generates a monotonic ULID with node ID incorporated.
     * This method is safe for distributed systems as it includes
     * the node ID in the random part of the ULID.
     *
     * @return a new monotonic ULID as a String
     */
    public String generateMonotonic() {
        MONOTONIC_LOCK.lock();
        try {
            long timestamp = System.currentTimeMillis();

            // If timestamp is the same as last one, increment the random part
            if (timestamp == lastTimestamp) {
                incrementRandomness(lastRandomness);
            } else {
                // If timestamp is different, generate new random values
                lastTimestamp = timestamp;

                // Generate random bytes
                secureRandom.nextBytes(lastRandomness);

                // Incorporate node ID into the first bytes of randomness
                // This ensures different nodes generate different ULIDs even with the same timestamp
                incorporateNodeId(lastRandomness, nodeId);
            }

            char[] ulid = new char[26]; // 10 (timestamp) + 16 (randomness)

            // Encode timestamp (10 characters)
            encodeTimestamp(timestamp, ulid, 0);

            // Encode randomness (16 characters)
            encodeBytes(lastRandomness, ulid, 10);

            return new String(ulid);
        } finally {
            MONOTONIC_LOCK.unlock();
        }
    }

    /**
     * Encode the timestamp part of a ULID.
     *
     * @param timestamp the timestamp in milliseconds
     * @param ulid      the char array to encode into
     * @param offset    the offset in the char array
     */
    private static void encodeTimestamp(long timestamp, char[] ulid, int offset) {
        // Encode timestamp (10 characters) - 48 bits
        for (int i = 9; i >= 0; i--) {
            int index = (int) (timestamp & 0x1F);
            ulid[offset + i] = ENCODING_CHARS[index];
            timestamp >>>= 5;
        }
    }

    /**
     * Incorporate node ID into the random bytes.
     *
     * @param bytes  the random bytes
     * @param nodeId the node ID to incorporate
     */
    private void incorporateNodeId(byte[] bytes, int nodeId) {
        // Replace first 10 bits with node ID
        // This keeps the remaining 70 bits for randomness
        bytes[0] = (byte) ((bytes[0] & 0x03) | ((nodeId >> 8) << 2));
        bytes[1] = (byte) (nodeId & 0xFF);
    }

    /**
     * Increment the random bytes for monotonic generation.
     *
     * @param bytes the bytes to increment
     */
    private void incrementRandomness(byte[] bytes) {
        for (int i = bytes.length - 1; i >= 2; i--) {
            // Start from index 2 to preserve node ID in first two bytes
            if ((bytes[i] & 0xFF) == 0xFF) {
                bytes[i] = 0;
            } else {
                bytes[i]++;
                break;
            }
        }
    }

    /**
     * Encode bytes using Crockford's base32.
     *
     * @param bytes  the bytes to encode
     * @param ulid   the char array to encode into
     * @param offset the offset in the char array
     */
    private void encodeBytes(byte[] bytes, char[] ulid, int offset) {
        // First byte (8 bits) -> 2 characters (10 bits)
        ulid[offset] = ENCODING_CHARS[(bytes[0] & 0xFF) >>> 3];
        ulid[offset + 1] = ENCODING_CHARS[((bytes[0] & 0x07) << 2) | ((bytes[1] & 0xFF) >>> 6)];

        // Second byte (6 remaining bits) + third byte (2 bits) -> 2 characters
        ulid[offset + 2] = ENCODING_CHARS[((bytes[1] & 0x3F) >>> 1)];
        ulid[offset + 3] = ENCODING_CHARS[((bytes[1] & 0x01) << 4) | ((bytes[2] & 0xFF) >>> 4)];

        // Continue with the pattern
        ulid[offset + 4] = ENCODING_CHARS[((bytes[2] & 0x0F) << 1) | ((bytes[3] & 0xFF) >>> 7)];
        ulid[offset + 5] = ENCODING_CHARS[((bytes[3] & 0x7F) >>> 2)];
        ulid[offset + 6] = ENCODING_CHARS[((bytes[3] & 0x03) << 3) | ((bytes[4] & 0xFF) >>> 5)];
        ulid[offset + 7] = ENCODING_CHARS[(bytes[4] & 0x1F)];

        ulid[offset + 8] = ENCODING_CHARS[(bytes[5] & 0xFF) >>> 3];
        ulid[offset + 9] = ENCODING_CHARS[((bytes[5] & 0x07) << 2) | ((bytes[6] & 0xFF) >>> 6)];

        ulid[offset + 10] = ENCODING_CHARS[((bytes[6] & 0x3F) >>> 1)];
        ulid[offset + 11] = ENCODING_CHARS[((bytes[6] & 0x01) << 4) | ((bytes[7] & 0xFF) >>> 4)];

        ulid[offset + 12] = ENCODING_CHARS[((bytes[7] & 0x0F) << 1) | ((bytes[8] & 0xFF) >>> 7)];
        ulid[offset + 13] = ENCODING_CHARS[((bytes[8] & 0x7F) >>> 2)];
        ulid[offset + 14] = ENCODING_CHARS[((bytes[8] & 0x03) << 3) | ((bytes[9] & 0xFF) >>> 5)];
        ulid[offset + 15] = ENCODING_CHARS[(bytes[9] & 0x1F)];
    }

    public int getNodeId() {
        return nodeId;
    }
}