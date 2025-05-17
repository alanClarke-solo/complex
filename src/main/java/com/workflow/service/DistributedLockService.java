package com.workflow.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DistributedLockService {
    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "workflow-lock:";
    private static final long DEFAULT_TIMEOUT = 60; // seconds

    public boolean acquireLock(String resourceId, String ownerId) {
        return acquireLock(resourceId, ownerId, DEFAULT_TIMEOUT);
    }

    public boolean acquireLock(String resourceId, String ownerId, long timeout) {
        String lockKey = LOCK_PREFIX + resourceId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            return lock.tryLock(0, timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void releaseLock(String resourceId) {
        String lockKey = LOCK_PREFIX + resourceId;
        RLock lock = redissonClient.getLock(lockKey);

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public void forceLock(String resourceId) {
        String lockKey = LOCK_PREFIX + resourceId;
        RLock lock = redissonClient.getLock(lockKey);

        if (lock.isLocked()) {
            lock.forceUnlock();
        }
    }
}