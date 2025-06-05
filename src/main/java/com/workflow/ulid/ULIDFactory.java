package com.workflow.ulid;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Factory for generating ULIDs with a fixed node ID component.
 * This class is used for local generation of ULIDs when a node ID
 * has been assigned by the ULID service.
 */
@Slf4j
public class ULIDFactory {

    private static final char[] ENCODING_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
            'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X',
            'Y', 'Z'
    };

    private static final int TIMESTAMP_LEN = 10;
    private static final int RANDOM_LEN = 16;
    private static final int ULID_LEN = TIMESTAMP_LEN + RANDOM_LEN;

    @Getter
    private final int nodeId;

    private final SecureRandom random;
    private long lastTimestamp = -1;
    private final byte[] lastRandomness = new byte[10];

    /**
     * Creates a new ULID factory with the given node ID.
     *
     * @param nodeId the node ID (0-1023)
     * @throws IllegalArgumentException if the node ID is outside the valid range
     */
    public ULIDFactory(int nodeId) {
        if (nodeId < 0 || nodeId > 1023) {
            throw new IllegalArgumentException("Node ID must be between 0 and 1023");
        }

        this.nodeId = nodeId;
        this.random = new SecureRandom();
    }

    /**
     * Generate a new ULID.
     *
     * @return a new ULID as a String
     */
    public String generate() {
        long timestamp = Instant.now().toEpochMilli();

        // Generate random values
        byte[] randomness = new byte[10];
        random.nextBytes(randomness);

        // Incorporate node ID into the random component
        incorporateNodeId(randomness, nodeId);

        // Save for monotonic generation
        System.arraycopy(randomness, 0, lastRandomness, 0, randomness.length);
        lastTimestamp = timestamp;

        return createULID(timestamp, randomness);
    }

    /**
     * Generate a monotonic ULID.
     *
     * @return a new monotonic ULID as a String
     */
    public String generateMonotonic() {
        long timestamp = Instant.now().toEpochMilli();

        // If same timestamp as last time, increment the randomness
        if (timestamp <= lastTimestamp) {
            timestamp = lastTimestamp;
            incrementRandomness(lastRandomness);
        } else {
            // If timestamp is different, generate new random values
            random.nextBytes(lastRandomness);

            // Incorporate node ID into the random component
            incorporateNodeId(lastRandomness, nodeId);
        }

        // Update last timestamp
        lastTimestamp = timestamp;

        return createULID(timestamp, lastRandomness);
    }

    /**
     * Create a ULID string from timestamp and randomness.
     *
     * @param timestamp the timestamp in milliseconds
     * @param randomness the random bytes
     * @return the ULID string
     */
    private String createULID(long timestamp, byte[] randomness) {
        char[] ulid = new char[ULID_LEN];

        // Encode timestamp (10 characters)
        encodeTimestamp(timestamp, ulid, 0);

        // Encode randomness with embedded node ID (16 characters)
        encodeRandomness(randomness, ulid, TIMESTAMP_LEN);

        return new String(ulid);
    }

    /**
     * Encode the timestamp part of a ULID.
     *
     * @param timestamp the timestamp in milliseconds
     * @param ulid the char array to encode into
     * @param offset the offset in the char array
     */
    private void encodeTimestamp(long timestamp, char[] ulid, int offset) {
        for (int i = TIMESTAMP_LEN - 1; i >= 0; i--) {
            int mod = (int) (timestamp % 32);
            ulid[offset + i] = ENCODING_CHARS[mod];
            timestamp /= 32;
        }
    }

    /**
     * Encode the randomness part of a ULID.
     *
     * @param randomness the random bytes
     * @param ulid the char array to encode into
     * @param offset the offset in the char array
     */
    private void encodeRandomness(byte[] randomness, char[] ulid, int offset) {
        // Encode each 5 bits from the randomness as a character
        int index = 0;

        // Handle the first byte specially (8 bits -> first 5 bits + 3 bits for next char)
        ulid[offset + index++] = ENCODING_CHARS[(randomness[0] & 0xF8) >>> 3];

        // Handle the overlapping bits
        ulid[offset + index++] = ENCODING_CHARS[((randomness[0] & 0x07) << 2) | ((randomness[1] & 0xC0) >>> 6)];
        ulid[offset + index++] = ENCODING_CHARS[(randomness[1] & 0x3E) >>> 1];
        ulid[offset + index++] = ENCODING_CHARS[((randomness[1] & 0x01) << 4) | ((randomness[2] & 0xF0) >>> 4)];
        ulid[offset + index++] = ENCODING_CHARS[((randomness[2] & 0x0F) << 1) | ((randomness[3] & 0x80) >>> 7)];
        ulid[offset + index++] = ENCODING_CHARS[(randomness[3] & 0x7C) >>> 2];
        ulid[offset + index++] = ENCODING_CHARS[((randomness[3] & 0x03) << 3) | ((randomness[4] & 0xE0) >>> 5)];
        ulid[offset + index++] = ENCODING_CHARS[randomness[4] & 0x1F];

        ulid[offset + index++] = ENCODING_CHARS[(randomness[5] & 0xF8) >>> 3];
        ulid[offset + index++] = ENCODING_CHARS[((randomness[5] & 0x07) << 2) | ((randomness[6] & 0xC0) >>> 6)];
        ulid[offset + index++] = ENCODING_CHARS[(randomness[6] & 0x3E) >>> 1];
        ulid[offset + index++] = ENCODING_CHARS[((randomness[6] & 0x01) << 4) | ((randomness[7] & 0xF0) >>> 4)];
        ulid[offset + index++] = ENCODING_CHARS[((randomness[7] & 0x0F) << 1) | ((randomness[8] & 0x80) >>> 7)];
        ulid[offset + index++] = ENCODING_CHARS[(randomness[8] & 0x7C) >>> 2];
        ulid[offset + index++] = ENCODING_CHARS[((randomness[8] & 0x03) << 3) | ((randomness[9] & 0xE0) >>> 5)];
        ulid[offset + index] = ENCODING_CHARS[randomness[9] & 0x1F];
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
     * Increment the randomness for the monotonic generation.
     *
     * @param bytes the bytes to increment
     */
    private void incrementRandomness(byte[] bytes) {
        // Skip first 2 bytes as they contain the node ID
        for (int i = bytes.length - 1; i >= 2; i--) {
            if ((bytes[i] & 0xFF) == 0xFF) {
                bytes[i] = 0;
            } else {
                bytes[i]++;
                break;
            }
        }
    }

    /**
     * Extract the node ID from a ULID string.
     *
     * @param ulid The ULID string to extract the node ID from
     * @return The node ID (0-1023)
     * @throws IllegalArgumentException if the ULID string is invalid
     */
    public static int extractNodeId(String ulid) {
        if (ulid == null) {
            throw new IllegalArgumentException("ULID cannot be null");
        }

        if (ulid.length() != 26) {
            throw new IllegalArgumentException("Invalid ULID length: must be 26 characters");
        }

        // Check if ULID contains only valid characters
        if (!ulid.matches("[0-9A-HJKMNP-TV-Z]{26}")) {
            throw new IllegalArgumentException("ULID contains invalid characters");
        }

        // Decode first two characters of the random part (after 10 timestamp chars)
        char[] encodedRandom = ulid.substring(10, 12).toCharArray();

        // Convert the characters to their 5-bit values
        int bits0 = decodeChar(encodedRandom[0]);  // First 5 bits
        int bits1 = decodeChar(encodedRandom[1]);  // Next 5 bits

        // Convert these values back to the original byte representation
        byte byte0 = (byte) ((bits0 << 3) | (bits1 >> 2));

        // From byte0, we extract the high 2 bits of the node ID (bits 2-3)
        int highBits = (byte0 >> 2) & 0x03;

        // We need additional characters to get the low 8 bits
        int bits2 = decodeChar(ulid.charAt(12));  // Next 5 bits
        int bits3 = decodeChar(ulid.charAt(13));  // Next 5 bits

        // byte1 is constructed from the last 3 bits of bits1 and first 5 bits of bits2
        byte byte1 = (byte) (((bits1 & 0x03) << 6) | ((bits2 & 0x1F) << 1) | ((bits3 & 0x10) >> 4));

        // Combine to get the full node ID
        return (highBits << 8) | (byte1 & 0xFF);
    }

    /**
     * Decodes a character from the Crockford Base32 encoding.
     *
     * @param c The character to decode
     * @return The 5-bit value
     * @throws IllegalArgumentException if the character is not valid
     */
    private static int decodeChar(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'H') {
            return c - 'A' + 10;
        } else if (c >= 'J' && c <= 'K') {
            return c - 'J' + 18;
        } else if (c >= 'M' && c <= 'N') {
            return c - 'M' + 20;
        } else if (c >= 'P' && c <= 'T') {
            return c - 'P' + 22;
        } else if (c >= 'V' && c <= 'Z') {
            return c - 'V' + 27;
        } else {
            throw new IllegalArgumentException("Invalid character in ULID: " + c);
        }
    }
}