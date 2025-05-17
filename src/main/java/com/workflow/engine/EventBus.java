package com.workflow.engine;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class EventBus {
    private final Map<String, CopyOnWriteArrayList<Consumer<Map<String, Object>>>> subscribers =
            new ConcurrentHashMap<>();

    private final RedissonClient redissonClient;
    private final Map<String, Integer> listenerIds = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Subscribe to global events channel
        RTopic topic = redissonClient.getTopic("workflow-events");

        int listenerId = topic.addListener(Map.class, (channel, message) -> {
            // Extract event type from the message
            String eventType = (String) message.get("eventType");
            if (eventType != null) {
                // Notify local subscribers
                notifySubscribers(eventType, message);
            }
        });

        listenerIds.put("workflow-events", listenerId);
    }

    @PreDestroy
    public void shutdown() {
        // Unsubscribe from topics
        listenerIds.forEach((channel, id) -> {
            RTopic topic = redissonClient.getTopic(channel);
            topic.removeListener(id);
        });
    }

    public void subscribe(String eventType, Consumer<Map<String, Object>> handler) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    public void publish(String eventType, Map<String, Object> eventData) {
        // Add event type to the message
        eventData.put("eventType", eventType);

        // Publish to Redis for distributed event handling
        RTopic topic = redissonClient.getTopic("workflow-events");
        topic.publish(eventData);

        // Handle locally
        notifySubscribers(eventType, eventData);
    }

    private void notifySubscribers(String eventType, Map<String, Object> eventData) {
        subscribers.getOrDefault(eventType, new CopyOnWriteArrayList<>())
                .forEach(subscriber -> subscriber.accept(eventData));
    }
}