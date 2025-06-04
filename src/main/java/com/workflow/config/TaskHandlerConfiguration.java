package com.workflow.config;

import com.workflow.service.TaskExecutionService;
import com.workflow.service.task.HttpTaskHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TaskHandlerConfiguration {

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private HttpTaskHandler httpTaskHandler;

    /**
     * Register all task handlers.
     */
    @PostConstruct
    public void registerTaskHandlers() {
        log.info("Registering task handlers");

        // Register HTTP task handler
        taskExecutionService.registerTaskHandler("HTTP", httpTaskHandler);

        // Register additional task handlers here

        log.info("Task handlers registered");
    }
}