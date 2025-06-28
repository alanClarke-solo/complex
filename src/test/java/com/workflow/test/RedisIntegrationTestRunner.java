package com.workflow.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.engine.EventBus;
import com.workflow.service.DistributedLockService;
import com.workflow.service.DistributedSemaphoreService;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive test runner for Redis integration using a Redisson client.
 * This class demonstrates and tests all Redis-related functionality in the workflow system.
 * 
 * To run this test:
 * 1. Start a Redis server on localhost:6379
 * 2. Run this class with the main method
 */
public class RedisIntegrationTestRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisIntegrationTestRunner.class);
    
    private RedissonClient redissonClient;
    private CacheManager cacheManager;
    private RedisTemplate<String, Object> redisTemplate;
    private DistributedLockService lockService;
    private DistributedSemaphoreService semaphoreService;
    private EventBus eventBus;
    
    public static void main(String[] args) {
        RedisIntegrationTestRunner testRunner = new RedisIntegrationTestRunner();
        try {
            testRunner.initialize();
            testRunner.runAllTests();
        } catch (Exception e) {
            logger.error("Test execution failed", e);
        } finally {
            testRunner.cleanup();
        }
    }
    
    private void initialize() {
        logger.info("Initializing Redis test environment...");
        
        // Initialize Redisson client
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://localhost:6379")
              .setConnectTimeout(3000)
              .setTimeout(3000);
        
        redissonClient = Redisson.create(config);
        
        // Initialize cache manager
        Map<String, CacheConfig> cacheConfigs = new HashMap<>();
        cacheConfigs.put("workflows", new CacheConfig(86400000, 0)); // 24 hours
        cacheConfigs.put("workflowExecutions", new CacheConfig(1800000, 0)); // 30 minutes
        cacheConfigs.put("testCache", new CacheConfig(60000, 0)); // 1 minute for testing
        
        cacheManager = new RedissonSpringCacheManager(redissonClient, cacheConfigs);
        
        // Initialize Redis template (simulate Spring Data Redis template)
        redisTemplate = createRedisTemplate();
        
        // Initialize services
        lockService = new DistributedLockService(redissonClient);
        semaphoreService = new DistributedSemaphoreService(redissonClient);
        eventBus = new EventBus(redissonClient);
        
        logger.info("Redis test environment initialized successfully");
    }
    
    private RedisTemplate<String, Object> createRedisTemplate() {
        // Simulate RedisTemplate creation
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        // Note: In real Spring app, connection factory would be set here
        return template;
    }
    
    private void runAllTests() {
        logger.info("Starting Redis integration tests...");
        
        testBasicRedisOperations();
        testDistributedLocking();
        testDistributedSemaphore();
        testCacheOperations();
        testEventBus();
        testRedisDataStructures();
        testConcurrentOperations();
        testTransactionOperations();
        
        logger.info("All Redis integration tests completed");
    }
    
    private void testBasicRedisOperations() {
        logger.info("Testing basic Redis operations...");
        
        try {
            // Test basic string operations
            RBucket<String> bucket = redissonClient.getBucket("test:basic:string");
            bucket.set("Hello Redis!");
            String value = bucket.get();
            logger.info("String operation: {}", value);
            assert "Hello Redis!".equals(value);
            
            // Test expiration
            RBucket<String> expiringBucket = redissonClient.getBucket("test:basic:expiring");
            expiringBucket.set("This will expire", 2, TimeUnit.SECONDS);
            logger.info("Expiring value set, waiting 3 seconds...");
            Thread.sleep(3000);
            String expiredValue = expiringBucket.get();
            logger.info("Expired value: {}", expiredValue);
            assert expiredValue == null;
            
            // Test JSON objects
            TestWorkflow workflow = new TestWorkflow("wf-001", "Test Workflow", LocalDateTime.now());
            RBucket<TestWorkflow> objectBucket = redissonClient.getBucket("test:basic:object");
            objectBucket.set(workflow);
            TestWorkflow retrievedWorkflow = objectBucket.get();
            logger.info("Object operation: {}", retrievedWorkflow);
            assert workflow.getWorkflowId().equals(retrievedWorkflow.getWorkflowId());
            
            logger.info("✓ Basic Redis operations test passed");
        } catch (Exception e) {
            logger.error("✗ Basic Redis operations test failed", e);
        }
    }
    
    private void testDistributedLocking() {
        logger.info("Testing distributed locking...");
        
        try {
            String resourceId = "test-resource-" + System.currentTimeMillis();
            
            // Test single lock acquisition
            boolean acquired = lockService.acquireLock(resourceId, "client-1");
            logger.info("Lock acquired: {}", acquired);
            assert acquired;
            
            // Test concurrent lock attempts
            ExecutorService executor = Executors.newFixedThreadPool(3);
            AtomicInteger successCount = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(3);
            
            for (int i = 0; i < 3; i++) {
                final int clientId = i + 2;
                executor.submit(() -> {
                    try {
                        boolean lockAcquired = lockService.acquireLock(resourceId, "client-" + clientId, 1);
                        if (lockAcquired) {
                            successCount.incrementAndGet();
                            logger.info("Client {} acquired lock", clientId);
                            lockService.releaseLock(resourceId, "client-" + clientId);
                        } else {
                            logger.info("Client {} failed to acquire lock", clientId);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Release the initial lock after 2 seconds
            Thread.sleep(2000);
            lockService.releaseLock(resourceId, "client-1");
            logger.info("Initial lock released");
            
            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();
            
            logger.info("Concurrent lock attempts - successful acquisitions: {}", successCount.get());
            assert successCount.get() >= 1; // At least one should succeed after release
            
            logger.info("✓ Distributed locking test passed");
        } catch (Exception e) {
            logger.error("✗ Distributed locking test failed", e);
        }
    }
    
    private void testDistributedSemaphore() {
        logger.info("Testing distributed semaphore...");
        
        try {
            String semaphoreId = "test-semaphore-" + System.currentTimeMillis();
            
            // Initialize semaphore with 2 permits
            semaphoreService.setPermits(semaphoreId, 2);
            
            // Test permit acquisition
            boolean acquired1 = semaphoreService.acquire(semaphoreId, 1, 5);
            boolean acquired2 = semaphoreService.acquire(semaphoreId, 1, 5);
            boolean acquired3 = semaphoreService.acquire(semaphoreId, 1, 1); // Should time out
            
            logger.info("Semaphore acquisitions: {}, {}, {}", acquired1, acquired2, acquired3);
            assert acquired1 && acquired2 && !acquired3;
            
            // Check available permits
            int available = semaphoreService.availablePermits(semaphoreId);
            logger.info("Available permits: {}", available);
            assert available == 0;
            
            // Release permits
            semaphoreService.release(semaphoreId, 1);
            available = semaphoreService.availablePermits(semaphoreId);
            logger.info("Available permits after release: {}", available);
            assert available == 1;
            
            logger.info("✓ Distributed semaphore test passed");
        } catch (Exception e) {
            logger.error("✗ Distributed semaphore test failed", e);
        }
    }
    
    private void testCacheOperations() {
        logger.info("Testing cache operations...");
        
        try {
            Cache testCache = cacheManager.getCache("testCache");
            assert testCache != null;
            
            // Test cache put and get
            TestWorkflow workflow = new TestWorkflow("wf-cache-001", "Cached Workflow", LocalDateTime.now());
            testCache.put("workflow-1", workflow);
            
            Cache.ValueWrapper wrapper = testCache.get("workflow-1");
            assert wrapper != null;
            TestWorkflow cachedWorkflow = (TestWorkflow) wrapper.get();
            logger.info("Cached workflow: {}", cachedWorkflow);
            assert workflow.getWorkflowId().equals(cachedWorkflow.getWorkflowId());
            
            // Test cache eviction
            testCache.evict("workflow-1");
            Cache.ValueWrapper evictedWrapper = testCache.get("workflow-1");
            assert evictedWrapper == null;
            
            // Test cache clear
            testCache.put("workflow-2", workflow);
            testCache.put("workflow-3", workflow);
            testCache.clear();
            
            assert testCache.get("workflow-2") == null;
            assert testCache.get("workflow-3") == null;
            
            logger.info("✓ Cache operations test passed");
        } catch (Exception e) {
            logger.error("✗ Cache operations test failed", e);
        }
    }
    
    private void testEventBus() {
        logger.info("Testing event bus operations...");
        
        try {
            CountDownLatch eventLatch = new CountDownLatch(2);
            AtomicInteger eventCount = new AtomicInteger(0);
            
            // Subscribe to workflow events
            eventBus.subscribe("WORKFLOW_STARTED", eventData -> {
                logger.info("Received WORKFLOW_STARTED event: {}", eventData);
                eventCount.incrementAndGet();
                eventLatch.countDown();
            });
            
            eventBus.subscribe("WORKFLOW_COMPLETED", eventData -> {
                logger.info("Received WORKFLOW_COMPLETED event: {}", eventData);
                eventCount.incrementAndGet();
                eventLatch.countDown();
            });
            
            // Wait a moment for subscriptions to be established
            Thread.sleep(1000);
            
            // Publish events
            Map<String, Object> startEvent = Map.of(
                "workflowId", "wf-event-001",
                "timestamp", LocalDateTime.now().toString(),
                "status", "STARTED"
            );
            eventBus.publish("WORKFLOW_STARTED", new HashMap<>(startEvent));
            
            Map<String, Object> completeEvent = Map.of(
                "workflowId", "wf-event-001",
                "timestamp", LocalDateTime.now().toString(),
                "status", "COMPLETED"
            );
            eventBus.publish("WORKFLOW_COMPLETED", new HashMap<>(completeEvent));
            
            // Wait for events to be processed
            boolean eventsReceived = eventLatch.await(10, TimeUnit.SECONDS);
            logger.info("Events received within timeout: {}, Event count: {}", eventsReceived, eventCount.get());
            assert eventsReceived && eventCount.get() == 2;
            
            logger.info("✓ Event bus test passed");
        } catch (Exception e) {
            logger.error("✗ Event bus test failed", e);
        }
    }
    
    private void testRedisDataStructures() {
        logger.info("Testing Redis data structures...");
        
        try {
            // Test Redis List
            RList<String> taskQueue = redissonClient.getList("test:queue:tasks");
            taskQueue.add("task-1");
            taskQueue.add("task-2");
            taskQueue.add("task-3");
            
            logger.info("Queue size: {}", taskQueue.size());
            assert taskQueue.size() == 3;
            
            String firstTask = taskQueue.remove(0);
            logger.info("Dequeued task: {}", firstTask);
            assert "task-1".equals(firstTask);
            
            // Test Redis Set
            RSet<String> activeWorkflows = redissonClient.getSet("test:set:active-workflows");
            activeWorkflows.add("wf-001");
            activeWorkflows.add("wf-002");
            activeWorkflows.add("wf-001"); // Duplicate should be ignored
            
            logger.info("Active workflows count: {}", activeWorkflows.size());
            assert activeWorkflows.size() == 2;
            assert activeWorkflows.contains("wf-001");
            
            // Test Redis Map
            RMap<String, String> workflowStatus = redissonClient.getMap("test:map:workflow-status");
            workflowStatus.put("wf-001", "RUNNING");
            workflowStatus.put("wf-002", "COMPLETED");
            
            logger.info("Workflow statuses: {}", workflowStatus.readAllMap());
            assert "RUNNING".equals(workflowStatus.get("wf-001"));
            
            // Test Redis Sorted Set
            RScoredSortedSet<String> priorityQueue = redissonClient.getScoredSortedSet("test:sorted-set:priority");
            priorityQueue.add(1.0, "low-priority-task");
            priorityQueue.add(5.0, "high-priority-task");
            priorityQueue.add(3.0, "medium-priority-task");
            
            String highestPriority = priorityQueue.last();
            logger.info("Highest priority task: {}", highestPriority);
            assert "high-priority-task".equals(highestPriority);
            
            logger.info("✓ Redis data structures test passed");
        } catch (Exception e) {
            logger.error("✗ Redis data structures test failed", e);
        }
    }
    
    private void testConcurrentOperations() {
        logger.info("Testing concurrent operations...");
        
        try {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(10);
            AtomicInteger counter = new AtomicInteger(0);
            
            RAtomicLong redisCounter = redissonClient.getAtomicLong("test:concurrent:counter");
            redisCounter.set(0);
            
            // Run 10 concurrent operations
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    try {
                        // Each thread increments both counters
                        counter.incrementAndGet();
                        redisCounter.incrementAndGet();
                        
                        // Simulate some work
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await(10, TimeUnit.SECONDS);
            executor.shutdown();
            
            long localCount = counter.get();
            long redisCount = redisCounter.get();
            
            logger.info("Local counter: {}, Redis counter: {}", localCount, redisCount);
            assert localCount == 10 && redisCount == 10;
            
            logger.info("✓ Concurrent operations test passed");
        } catch (Exception e) {
            logger.error("✗ Concurrent operations test failed", e);
        }
    }
    
    private void testTransactionOperations() {
        logger.info("Testing transaction operations...");
        
        try {
            RTransaction transaction = redissonClient.createTransaction(TransactionOptions.defaults());
            
            RBucket<String> bucket1 = transaction.getBucket("test:tx:bucket1");
            RBucket<String> bucket2 = transaction.getBucket("test:tx:bucket2");
            
            bucket1.set("value1");
            bucket2.set("value2");
            
            // Commit transaction
            transaction.commit();
            
            // Verify values were committed
            String value1 = redissonClient.getBucket("test:tx:bucket1").get().toString();
            String value2 = redissonClient.getBucket("test:tx:bucket2").get().toString();
            
            logger.info("Transaction values - value1: {}, value2: {}", value1, value2);
            assert "value1".equals(value1) && "value2".equals(value2);
            
            // Test rollback
            RTransaction rollbackTx = redissonClient.createTransaction(TransactionOptions.defaults());
            RBucket<String> bucket3 = rollbackTx.getBucket("test:tx:bucket3");
            bucket3.set("will-be-rolled-back");
            rollbackTx.rollback();
            
            String value3 = redissonClient.getBucket("test:tx:bucket3").get().toString();
            logger.info("Rolled back value: {}", value3);
            assert value3 == null;
            
            logger.info("✓ Transaction operations test passed");
        } catch (Exception e) {
            logger.error("✗ Transaction operations test failed", e);
        }
    }
    
    private void cleanup() {
        logger.info("Cleaning up Redis test environment...");
        
        try {
            // Clean up test keys
            redissonClient.getKeys().deleteByPattern("test:*");
            
            // Shutdown event bus
            if (eventBus != null) {
                eventBus.shutdown();
            }
            
            // Shutdown Redisson client
            if (redissonClient != null) {
                redissonClient.shutdown();
            }
            
            logger.info("Redis test environment cleaned up successfully");
        } catch (Exception e) {
            logger.error("Error during cleanup", e);
        }
    }
    
    // Test data class
    public static class TestWorkflow {
        private String workflowId;
        private String name;
        private LocalDateTime createdAt;
        
        public TestWorkflow() {}
        
        public TestWorkflow(String workflowId, String name, LocalDateTime createdAt) {
            this.workflowId = workflowId;
            this.name = name;
            this.createdAt = createdAt;
        }
        
        public String getWorkflowId() { return workflowId; }
        public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        @Override
        public String toString() {
            return String.format("TestWorkflow{workflowId='%s', name='%s', createdAt=%s}", 
                                workflowId, name, createdAt);
        }
    }
}