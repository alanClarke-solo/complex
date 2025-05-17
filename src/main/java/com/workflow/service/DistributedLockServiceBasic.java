package com.workflow.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class DistributedLockServiceBasic {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String LOCK_PREFIX = "workflow-lock:";
    private static final long DEFAULT_TIMEOUT = 60; // seconds
    
    public DistributedLockServiceBasic(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public boolean acquireLock(String resourceId, String ownerId) {
        return acquireLock(resourceId, ownerId, DEFAULT_TIMEOUT);
    }
    
    public boolean acquireLock(String resourceId, String ownerId, long timeout) {
        String lockKey = LOCK_PREFIX + resourceId;
        
        Boolean acquired = redisTemplate.opsForValue()
                                      .setIfAbsent(lockKey, ownerId, timeout, TimeUnit.SECONDS);
        
        return Boolean.TRUE.equals(acquired);
    }
    
    public boolean releaseLock(String resourceId, String ownerId) {
        String lockKey = LOCK_PREFIX + resourceId;
        
        // Only release if we're the owner
        String currentOwner = redisTemplate.opsForValue().get(lockKey);
        
        if (ownerId.equals(currentOwner)) {
            redisTemplate.delete(lockKey);
            return true;
        }
        
        return false;
    }
}