package com.workflow.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Generator for Universally Unique Lexicographically Sortable Identifiers (ULIDs).
 * ULIDs are 128-bit identifiers that combine a timestamp with random data,
 * ensuring both uniqueness and lexicographical sortability.
 */
@Component
public class ULIDGenerator {
    private static final char[] ENCODING_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
            'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X',
            'Y', 'Z'
    };

    private static final int TIMESTAMP_LEN = 10;
    private static final int RANDOM_LEN = 16;
    private static final int ULID_LEN = TIMESTAMP_LEN + RANDOM_LEN;

    private final SecureRandom random;
    private long lastTimestamp = -1;
    private final byte[] lastRandomness = new byte[10];

    public ULIDGenerator() {
        this.random = new SecureRandom();
    }

    /**
     * Generate a new ULID.
     *
     * @return a new ULID as a String
     */
    public synchronized String generate() {
        long timestamp = Instant.now().toEpochMilli();

        // If same timestamp as last time, increment the randomness
        if (timestamp == lastTimestamp) {
            incrementRandomness(lastRandomness);
        } else {
            // If timestamp is different, generate new random values
            lastTimestamp = timestamp;
            random.nextBytes(lastRandomness);
        }

        char[] ulid = new char[ULID_LEN];

        // Encode timestamp (10 characters)
        encodeTimestamp(timestamp, ulid, 0);

        // Encode randomness (16 characters)
        encodeRandomness(lastRandomness, ulid, TIMESTAMP_LEN);

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
     * Increment the randomness for the monotonic generation.
     *
     * @param bytes the bytes to increment
     */
    private void incrementRandomness(byte[] bytes) {
        for (int i = bytes.length - 1; i >= 0; i--) {
            if ((bytes[i] & 0xFF) == 0xFF) {
                bytes[i] = 0;
            } else {
                bytes[i]++;
                break;
            }
        }
    }
}