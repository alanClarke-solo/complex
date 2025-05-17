package com.workflow.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DistributedSemaphoreService {
    private final RedissonClient redissonClient;
    private static final String SEMAPHORE_PREFIX = "workflow-semaphore:";

    public boolean acquire(String resourceId, int permits, long timeout) {
        String semaphoreKey = SEMAPHORE_PREFIX + resourceId;
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);

        try {
            return semaphore.tryAcquire(permits, timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release(String resourceId, int permits) {
        String semaphoreKey = SEMAPHORE_PREFIX + resourceId;
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);

        semaphore.release(permits);
    }

    public int availablePermits(String resourceId) {
        String semaphoreKey = SEMAPHORE_PREFIX + resourceId;
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);

        return semaphore.availablePermits();
    }

    public void setPermits(String resourceId, int permits) {
        String semaphoreKey = SEMAPHORE_PREFIX + resourceId;
        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);

        semaphore.trySetPermits(permits);
    }
}