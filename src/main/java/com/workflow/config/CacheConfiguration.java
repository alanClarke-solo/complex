package com.workflow.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Autowired
    private WorkflowConfiguration workflowConfig;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Define default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Create cache configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Workflow execution cache - longer TTL for completed workflows
        cacheConfigs.put("workflows", defaultConfig.entryTtl(Duration.ofDays(7)));

        // Task cache - shorter TTL
        cacheConfigs.put("tasks", defaultConfig.entryTtl(Duration.ofHours(2)));

        // Workflow definition cache - long TTL as these change infrequently
        cacheConfigs.put("workflowDefinitions", defaultConfig.entryTtl(Duration.ofDays(30)));

        // Apply any custom cache settings from configuration
        if (workflowConfig.getCache() != null) {
            for (Map.Entry<String, Object> entry : workflowConfig.getCache().entrySet()) {
                if (entry.getValue() instanceof Integer) {
                    // Interpret value as TTL in minutes
                    int ttlMinutes = (Integer) entry.getValue();
                    cacheConfigs.put(entry.getKey(), defaultConfig.entryTtl(Duration.ofMinutes(ttlMinutes)));
                }
            }
        }

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}