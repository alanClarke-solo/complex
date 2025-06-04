package com.workflow.service;

import com.workflow.model.Task;
import com.workflow.model.Workflow;
import com.workflow.util.WorkflowDAG;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WorkflowDAGService {

    // Cache of workflow DAGs
    private final Map<String, WorkflowDAG> dagCache = new ConcurrentHashMap<>();

    /**
     * Build a workflow DAG.
     *
     * @param workflow the workflow
     * @param tasks the tasks in the workflow
     * @return the workflow DAG
     */
    @Cacheable(value = "workflowDefinitions", key = "#workflow.workflowId", unless = "#result == null")
    public WorkflowDAG buildWorkflowDAG(Workflow workflow, List<Task> tasks) {
        String workflowId = workflow.getWorkflowId();

        // Check cache first
        WorkflowDAG cachedDag = dagCache.get(workflowId);
        if (cachedDag != null) {
            return cachedDag;
        }

        log.info("Building DAG for workflow: {} ({})", workflow.getName(), workflowId);

        // Create a new DAG
        WorkflowDAG dag = new WorkflowDAG();

        // Add all tasks as nodes
        for (Task task : tasks) {
            dag.addNode(task.getTaskId());
        }

        // Add edges based on task dependencies
        // This is a simplified implementation - in a real system, you would have
        // explicit dependency information in your task model
        establishTaskDependencies(dag, tasks);

        // Cache the DAG
        dagCache.put(workflowId, dag);

        return dag;
    }

    /**
     * Establish task dependencies.
     * This is a simplified implementation that looks for task dependencies
     * based on input/output parameters. In a real system, you would have
     * explicit dependency information in your task model.
     *
     * @param dag the DAG to build
     * @param tasks the tasks to analyze
     */
    private void establishTaskDependencies(WorkflowDAG dag, List<Task> tasks) {
        // This is a simplified example that establishes dependencies based on a convention
        // In a real system, you would have explicit dependency information

        for (Task task : tasks) {
            Map<String, Object> inputParams = task.getInputParameters();

            if (inputParams != null && inputParams.containsKey("dependsOn")) {
                Object dependsOnObj = inputParams.get("dependsOn");

                if (dependsOnObj instanceof String) {
                    String dependsOn = (String) dependsOnObj;
                    // Add edge from dependency to current task
                    dag.addEdge(dependsOn, task.getTaskId());
                } else if (dependsOnObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> dependencies = (List<String>) dependsOnObj;

                    for (String dependsOn : dependencies) {
                        // Add edge from each dependency to current task
                        dag.addEdge(dependsOn, task.getTaskId());
                    }
                }
            }
        }

        // After adding all edges, identify start nodes (nodes with no incoming edges)
        for (Task task : tasks) {
            if (dag.getIncomingEdges(task.getTaskId()).isEmpty()) {
                dag.markAsStartNode(task.getTaskId());
            }
        }
    }

    /**
     * Get a workflow DAG by workflow ID.
     *
     * @param workflowId the workflow ID
     * @return the workflow DAG, or null if not found
     */
    public WorkflowDAG getWorkflowDAG(String workflowId) {
        return dagCache.get(workflowId);
    }

    /**
     * Clear the DAG cache.
     *
     * @param workflowId the workflow ID
     */
    public void clearDAGCache(String workflowId) {
        dagCache.remove(workflowId);
        log.info("Cleared DAG cache for workflow: {}", workflowId);
    }
}