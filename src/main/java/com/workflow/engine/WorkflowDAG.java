package com.workflow.engine;

import com.workflow.model.Task;
import lombok.Data;

import java.util.*;

public class WorkflowDAG {
    private Map<String, DAGNode> nodes = new HashMap<>();
    private Map<String, List<String>> edges = new HashMap<>();

    public void addNode(String taskId, Task task) {
        nodes.put(taskId, new DAGNode(taskId, task));
        if (!edges.containsKey(taskId)) {
            edges.put(taskId, new ArrayList<>());
        }
    }

    public void addEdge(String fromTaskId, String toTaskId) {
        if (!nodes.containsKey(fromTaskId) || !nodes.containsKey(toTaskId)) {
            throw new IllegalArgumentException("Both tasks must exist in the DAG");
        }
        edges.get(fromTaskId).add(toTaskId);
    }

    public Set<String> getStartNodes() {
        Set<String> startNodes = new HashSet<>(nodes.keySet());
        for (List<String> destinations : edges.values()) {
            startNodes.removeAll(destinations);
        }
        return startNodes;
    }

    public List<String> getNextTasks(String taskId) {
        return edges.getOrDefault(taskId, Collections.emptyList());
    }

    public Task getTask(String taskId) {
        return nodes.get(taskId).getTask();
    }

    public boolean hasCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String taskId : nodes.keySet()) {
            if (detectCycle(taskId, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean detectCycle(String taskId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(taskId)) {
            return true;
        }

        if (visited.contains(taskId)) {
            return false;
        }

        visited.add(taskId);
        recursionStack.add(taskId);

        for (String nextTask : getNextTasks(taskId)) {
            if (detectCycle(nextTask, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(taskId);
        return false;
    }

    @Data
    private static class DAGNode {
        private final String taskId;
        private final Task task;
    }
}