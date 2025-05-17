package com.workflow.integration;

import com.workflow.engine.WorkflowManager;
import com.workflow.model.Workflow;
import com.workflow.model.WorkflowExecution;
import com.workflow.repository.WorkflowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class WorkflowExecutionIntegrationTest {
    
    @Autowired
    private WorkflowManager workflowManager;
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Test
    @Transactional
    void testWorkflowExecution() throws InterruptedException {
        // Given a workflow exists
        String workflowId = "test-workflow-" + UUID.randomUUID();
        createTestWorkflow(workflowId);
        
        // When we start the workflow
        WorkflowExecution execution = workflowManager.startWorkflow(workflowId);
        
        // Then the execution should be created
        assertNotNull(execution);
        assertNotNull(execution.getExecutionId());
        assertEquals(workflowId, execution.getWorkflowId());
        
        // Wait for workflow to complete
        int maxWaitTimeSeconds = 30;
        for (int i = 0; i < maxWaitTimeSeconds; i++) {
            execution = workflowManager.getWorkflowExecution(execution.getExecutionId());
            if (execution.getEndTime() != null) {
                break;
            }
            Thread.sleep(1000);
        }
        
        // Assert workflow completed
        assertNotNull(execution.getEndTime());
    }
    
    private void createTestWorkflow(String workflowId) {
        Workflow workflow = new Workflow();
        workflow.setWorkflowId(workflowId);
        workflow.setName("Test Workflow");
        workflow.setDescription("A workflow for testing");
        workflow.setParameters(new HashMap<>());
        
        workflowRepository.save(workflow);
        
        // Create tasks for this workflow would happen here
    }
}