package com.workflow.config;

import com.workflow.ulid.ULIDFactory;
import com.workflow.util.ULIDGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ULID generation in the workflow system.
 */
@Configuration
public class ULIDConfig {

    /**
     * Creates a ULIDGenerator bean for generating universally unique
     * lexicographically sortable identifiers.
     *
     * @return the ULIDGenerator instance
     */
    @Bean
    public ULIDGenerator ulidGenerator() {
        // Create a new ULIDGenerator instance
        return new ULIDGenerator();
    }
    
    /**
     * Creates a ULIDFactory bean with a fixed node ID.
     * This can be used for local ULID generation.
     *
     * @return the ULIDFactory instance
     */
    @Bean
    public ULIDFactory ulidFactory() {
        // Using a default node ID of 1 - in production this should be configured 
        // based on the deployment environment or obtained from a coordination service
        int nodeId = 1;
        return new ULIDFactory(nodeId);
    }
}