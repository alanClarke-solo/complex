package com.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RPermitExpirableSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {
    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "workflow-lock:";
    private static final Duration DEFAULT_LOCK_TTL = Duration.ofMinutes(10);
    private static final long DEFAULT_TIMEOUT = 30; // seconds

    // Track active locks for monitoring and cleanup
    private final Map<String, LockInfo> activeLocks = new ConcurrentHashMap<>();

    /**
     * Acquire lock with default timeout and TTL
     */
    public boolean acquireLock(String resourceId, String ownerId) {
        return acquireLock(resourceId, ownerId, DEFAULT_TIMEOUT);
    }

    /**
     * Acquire lock with custom timeout and default TTL
     */
    public boolean acquireLock(String resourceId, String ownerId, long timeout) {
        return acquireLock(resourceId, ownerId, timeout, DEFAULT_LOCK_TTL);
    }

    /**
     * Acquire lock with custom timeout and TTL
     */
    public boolean acquireLock(String resourceId, String ownerId, long timeout, Duration lockTTL) {
        String lockKey = LOCK_PREFIX + resourceId;
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(lockKey);

        try {
            // Initialize semaphore with 1 permit if it doesn't exist
            if (!semaphore.isExists()) {
                semaphore.trySetPermits(1);
            }

            // Try to acquire the permit with TTL
            String permitId = semaphore.tryAcquire(timeout, lockTTL.toSeconds(), TimeUnit.SECONDS);

            if (permitId != null) {
                // Lock acquired successfully
                LockInfo lockInfo = LockInfo.builder()
                        .resourceId(resourceId)
                        .ownerId(ownerId)
                        .permitId(permitId)
                        .acquiredAt(LocalDateTime.now())
                        .ttl(lockTTL)
                        .build();

                activeLocks.put(resourceId, lockInfo);

                log.debug("Lock acquired for resource {} by owner {} with TTL {} (permitId: {})",
                        resourceId, ownerId, lockTTL, permitId);
                return true;
            } else {
                log.debug("Failed to acquire lock for resource {} by owner {} within {} seconds",
                        resourceId, ownerId, timeout);
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while acquiring lock for resource {} by owner {}", resourceId, ownerId);
            return false;
        } catch (Exception e) {
            log.error("Error acquiring lock for resource {} by owner {}", resourceId, ownerId, e);
            return false;
        }
    }

    /**
     * Acquire lock with specific expiration time
     */
    public boolean acquireLockWithExpiration(String resourceId, String ownerId,
                                             long timeout, LocalDateTime expirationTime) {
        Duration ttl = Duration.between(LocalDateTime.now(), expirationTime);
        if (ttl.isNegative() || ttl.isZero()) {
            log.warn("Cannot acquire lock for resource {} - expiration time is in the past", resourceId);
            return false;
        }

        return acquireLock(resourceId, ownerId, timeout, ttl);
    }

    /**
     * Release lock
     */
    public boolean releaseLock(String resourceId, String ownerId) {
        LockInfo lockInfo = activeLocks.get(resourceId);

        if (lockInfo == null) {
            log.warn("No active lock found for resource {} by owner {}", resourceId, ownerId);
            return false;
        }

        if (!lockInfo.getOwnerId().equals(ownerId)) {
            log.warn("Lock ownership mismatch for resource {}. Expected: {}, Actual: {}",
                    resourceId, ownerId, lockInfo.getOwnerId());
            return false;
        }

        String lockKey = LOCK_PREFIX + resourceId;
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(lockKey);

        try {
            semaphore.release(lockInfo.getPermitId());
            activeLocks.remove(resourceId);

            log.debug("Lock released for resource {} by owner {} (permitId: {})",
                    resourceId, ownerId, lockInfo.getPermitId());
            return true;

        } catch (Exception e) {
            log.error("Error releasing lock for resource {} by owner {}", resourceId, ownerId, e);
            return false;
        }
    }

    /**
     * Force release lock (admin function)
     */
    public boolean forceReleaseLock(String resourceId) {
        LockInfo lockInfo = activeLocks.get(resourceId);

        if (lockInfo == null) {
            log.warn("No active lock found for resource {} to force release", resourceId);
            return false;
        }

        String lockKey = LOCK_PREFIX + resourceId;
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(lockKey);

        try {
            semaphore.release(lockInfo.getPermitId());
            activeLocks.remove(resourceId);

            log.warn("Lock force released for resource {} (was owned by: {}, permitId: {})",
                    resourceId, lockInfo.getOwnerId(), lockInfo.getPermitId());
            return true;

        } catch (Exception e) {
            log.error("Error force releasing lock for resource {}", resourceId, e);
            return false;
        }
    }

    /**
     * Check if lock is held
     */
    public boolean isLocked(String resourceId) {
        String lockKey = LOCK_PREFIX + resourceId;
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(lockKey);

        return semaphore.isExists() && semaphore.availablePermits() == 0;
    }

    /**
     * Check if lock is held by specific owner
     */
    public boolean isLockedBy(String resourceId, String ownerId) {
        LockInfo lockInfo = activeLocks.get(resourceId);
        return lockInfo != null && lockInfo.getOwnerId().equals(ownerId);
    }

    /**
     * Get lock information
     */
    public LockInfo getLockInfo(String resourceId) {
        return activeLocks.get(resourceId);
    }

    /**
     * Get available permits (should be 0 if locked, 1 if unlocked)
     */
    public int getAvailablePermits(String resourceId) {
        String lockKey = LOCK_PREFIX + resourceId;
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(lockKey);

        return semaphore.isExists() ? semaphore.availablePermits() : 1;
    }

    /**
     * Delete lock completely
     */
    public boolean deleteLock(String resourceId) {
        String lockKey = LOCK_PREFIX + resourceId;
        RPermitExpirableSemaphore semaphore = redissonClient.getPermitExpirableSemaphore(lockKey);

        boolean deleted = semaphore.delete();
        if (deleted) {
            activeLocks.remove(resourceId);
            log.debug("Lock deleted for resource {}", resourceId);
        }

        return deleted;
    }

    /**
     * Get all active locks
     */
    public Map<String, LockInfo> getAllActiveLocks() {
        return new ConcurrentHashMap<>(activeLocks);
    }

    /**
     * Scheduled cleanup of expired locks from local tracking
     */
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void cleanupExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();

        activeLocks.entrySet().removeIf(entry -> {
            String resourceId = entry.getKey();
            LockInfo lockInfo = entry.getValue();

            // Check if lock has expired based on acquisition time + TTL
            LocalDateTime expectedExpiry = lockInfo.getAcquiredAt().plus(lockInfo.getTtl());
            if (now.isAfter(expectedExpiry.plusMinutes(1))) { // Add 1 minute buffer
                log.debug("Removing tracking for expired lock: {} (expired at: {})",
                        resourceId, expectedExpiry);
                return true;
            }

            // Verify lock still exists in Redis
            if (!isLocked(resourceId)) {
                log.debug("Removing tracking for non-existent lock: {}", resourceId);
                return true;
            }

            return false;
        });

        log.debug("Lock cleanup completed. Tracking {} active locks", activeLocks.size());
    }

    /**
     * Lock information data class
     */
    @lombok.Builder
    @lombok.Data
    public static class LockInfo {
        private String resourceId;
        private String ownerId;
        private String permitId;
        private LocalDateTime acquiredAt;
        private Duration ttl;

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(acquiredAt.plus(ttl));
        }

        public Duration getRemainingTime() {
            LocalDateTime expiry = acquiredAt.plus(ttl);
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(expiry)) {
                return Duration.ZERO;
            }

            return Duration.between(now, expiry);
        }
    }
}