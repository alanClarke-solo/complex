/*
package com.workflow.engine;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class EventBus1 {
    private final Map<String, CopyOnWriteArrayList<Consumer<Map<String, Object>>>> subscribers = 
            new ConcurrentHashMap<>();
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public EventBus1(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        
        // Subscribe to Redis channels for distributed event handling
        redisTemplate.listenToChannel("workflow-events", message -> {
            // Handle incoming Redis events
            String channel = message.getChannel();
            Map<String, Object> payload = (Map<String, Object>) message.getMessage();
            
            // Notify local subscribers
            notifySubscribers(channel, payload);
        });
    }
    
    public void subscribe(String eventType, Consumer<Map<String, Object>> handler) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                  .add(handler);
    }
    
    public void publish(String eventType, Map<String, Object> eventData) {
        // Publish to Redis for distributed event handling
        redisTemplate.convertAndSend("workflow-events:" + eventType, eventData);
        
        // Handle locally
        notifySubscribers(eventType, eventData);
    }
    
    private void notifySubscribers(String eventType, Map<String, Object> eventData) {
        subscribers.getOrDefault(eventType, new CopyOnWriteArrayList<>())
                  .forEach(subscriber -> subscriber.accept(eventData));
    }
}*/
