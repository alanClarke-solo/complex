package com.workflow.util;

import com.workflow.ulid.ULIDFactory;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;

/**
 * ULID (Universally Unique Lexicographically Sortable Identifier) implementation.
 * <p>
 * ULID consists of:
 * - 48 bits of millisecond timestamp (supports dates until year 10889)
 * - 80 bits of randomness
 * <p>
 * This implementation provides both standard ULIDs and monotonic ULIDs
 * (which guarantee ascending lexicographical order even within the same timestamp).
 * <p>
 * This implementation is thread-safe and suitable for distributed systems.
 */
public class ULID {
    private static final char[] ENCODING_CHARS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K',
            'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'V', 'W', 'X',
            'Y', 'Z'
    };

    private static final byte[] DECODING_CHARS = new byte[128];
    private static final int MASK = 0x1F; // 31 (00011111)
    private static final int TIMESTAMP_LENGTH = 10; // 10 chars for timestamp
    private static final int RANDOMNESS_LENGTH = 16; // 16 chars for randomness
    private static final int ULID_LENGTH = TIMESTAMP_LENGTH + RANDOMNESS_LENGTH;
    private static final long MAX_TIME = (1L << 48) - 1;
    private static final ThreadLocal<MonotonicHolder> MONOTONIC_HOLDER = ThreadLocal.withInitial(MonotonicHolder::new);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        Arrays.fill(DECODING_CHARS, (byte) -1);
        for (int i = 0; i < ENCODING_CHARS.length; i++) {
            char c = ENCODING_CHARS[i];
            DECODING_CHARS[c] = (byte) i;
            // Also support lowercase letters
            DECODING_CHARS[Character.toLowerCase(c)] = (byte) i;
        }
    }

    private ULID() {
        // Utility class
    }

    /**
     * Generates a standard ULID using a secure random generator.
     *
     * @return a new ULID as a String
     */
    public static String generate() {
        return generate(System.currentTimeMillis(), SECURE_RANDOM);
    }

    /**
     * Generates a monotonic ULID that ensures lexicographical ordering
     * even within the same millisecond.
     *
     * @return a new monotonic ULID as a String
     */
    public static String generateMonotonic() {
        return generateMonotonic(System.currentTimeMillis());
    }

    /**
     * Generates a standard ULID with a specified timestamp.
     *
     * @param timestamp the timestamp in milliseconds
     * @return a new ULID as a String
     */
    public static String generate(long timestamp) {
        return generate(timestamp, SECURE_RANDOM);
    }

    /**
     * Generates a standard ULID with a specified timestamp and random source.
     *
     * @param timestamp the timestamp in milliseconds
     * @param random    the random source
     * @return a new ULID as a String
     */
    public static String generate(long timestamp, Random random) {
        validateTimestamp(timestamp);

        char[] ulid = new char[ULID_LENGTH];

        // Encode timestamp (10 characters)
        encodeTimestamp(timestamp, ulid, 0);

        // Encode randomness (16 characters)
        encodeRandomness(random, ulid, TIMESTAMP_LENGTH);

        return new String(ulid);
    }

    /**
     * Generates a monotonic ULID with a specified timestamp.
     * If the timestamp is the same as the previous call,
     * the random component is incremented.
     *
     * @param timestamp the timestamp in milliseconds
     * @return a new monotonic ULID as a String
     */
    public static String generateMonotonic(long timestamp) {
        validateTimestamp(timestamp);

        MonotonicHolder holder = MONOTONIC_HOLDER.get();

        // If timestamp is the same as last one, increment the random part
        if (timestamp == holder.lastTimestamp) {
            incrementRandomness(holder.lastRandomness);
        } else {
            // If timestamp is different, generate new random values
            holder.lastTimestamp = timestamp;
            SECURE_RANDOM.nextBytes(holder.lastRandomness);
        }

        char[] ulid = new char[ULID_LENGTH];

        // Encode timestamp (10 characters)
        encodeTimestamp(timestamp, ulid, 0);

        // Encode randomness (16 characters)
        encodeBytes(holder.lastRandomness, ulid, TIMESTAMP_LENGTH);

        return new String(ulid);
    }

    /**
     * Increment the random bytes for monotonic generation.
     *
     * @param bytes the bytes to increment
     */
    private static void incrementRandomness(byte[] bytes) {
        for (int i = bytes.length - 1; i >= 0; i--) {
            if ((bytes[i] & 0xFF) == 0xFF) {
                bytes[i] = 0;
            } else {
                bytes[i]++;
                break;
            }
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
            int index = (int) (timestamp & MASK);
            ulid[offset + i] = ENCODING_CHARS[index];
            timestamp >>>= 5;
        }
    }

    /**
     * Encode the random part of a ULID using a Random source.
     *
     * @param random the random source
     * @param ulid   the char array to encode into
     * @param offset the offset in the char array
     */
    private static void encodeRandomness(Random random, char[] ulid, int offset) {
        // 80 bits of randomness needs 16 characters in base32
        byte[] randomBytes = new byte[10]; // 80 bits = 10 bytes
        random.nextBytes(randomBytes);
        encodeBytes(randomBytes, ulid, offset);
    }

    /**
     * Encode bytes using Crockford's base32.
     *
     * @param bytes  the bytes to encode
     * @param ulid   the char array to encode into
     * @param offset the offset in the char array
     */
    private static void encodeBytes(byte[] bytes, char[] ulid, int offset) {
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

    /**
     * Parse a ULID string into its timestamp component.
     *
     * @param ulid the ULID string to parse
     * @return the timestamp in milliseconds
     * @throws IllegalArgumentException if the ULID is invalid
     */
    public static long getTimestamp(String ulid) {
        if (ulid == null || ulid.length() != ULID_LENGTH) {
            throw new IllegalArgumentException("Invalid ULID: " + ulid);
        }

        long timestamp = 0L;

        for (int i = 0; i < TIMESTAMP_LENGTH; i++) {
            char c = ulid.charAt(i);
            if (c >= DECODING_CHARS.length || DECODING_CHARS[c] == -1) {
                throw new IllegalArgumentException("Invalid ULID character: " + c);
            }
            timestamp = (timestamp << 5) | DECODING_CHARS[c];
        }

        return timestamp;
    }

    /**
     * Get the Instant representation of the ULID's timestamp.
     *
     * @param ulid the ULID string
     * @return the Instant representing the timestamp
     */
    public static Instant getInstant(String ulid) {
        return Instant.ofEpochMilli(getTimestamp(ulid));
    }

    /**
     * Validate that the timestamp is within the supported range.
     *
     * @param timestamp the timestamp to validate
     * @throws IllegalArgumentException if the timestamp is invalid
     */
    private static void validateTimestamp(long timestamp) {
        if (timestamp < 0 || timestamp > MAX_TIME) {
            throw new IllegalArgumentException(
                    "Timestamp must be between 0 and " + MAX_TIME + ", got: " + timestamp);
        }
    }

    /**
     * Holder class for monotonic generation state.
     */
    private static class MonotonicHolder {
        long lastTimestamp = -1L;
        byte[] lastRandomness = new byte[10]; // 80 bits = 10 bytes
    }

    public static void main(String[] args) {
// Generate a standard ULID
        String ulid = ULID.generate();
        System.out.println("Standard ULID: " + ulid);

// Generate a monotonic ULID
        String monotonicUlid = ULID.generateMonotonic();
        System.out.println("Monotonic ULID: " + monotonicUlid);

// Extract timestamp from a ULID
        long timestamp = ULID.getTimestamp(ulid);
        System.out.println("Timestamp: " + timestamp);
        System.out.println("Date: " + ULID.getInstant(ulid));

// For distributed services, use ULIDFactory with a unique node ID
        ULIDFactory nodeFactory = new ULIDFactory(42); // Node ID 42
        String distributedUlid = nodeFactory.generateMonotonic();
        System.out.println("Distributed ULID: " + distributedUlid);
    }
}