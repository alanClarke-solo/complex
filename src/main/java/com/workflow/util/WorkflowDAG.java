package com.workflow.util;

import java.io.Serializable;
import java.util.*;

/**
 * Directed Acyclic Graph (DAG) for workflow execution.
 */
public class WorkflowDAG implements Serializable {
    private static final long serialVersionUID = 1L;

    // Nodes in the graph (task IDs)
    private final Set<String> nodes = new HashSet<>();

    // Outgoing edges: taskId -> set of downstream task IDs
    private final Map<String, Set<String>> outgoingEdges = new HashMap<>();

    // Incoming edges: taskId -> set of upstream task IDs
    private final Map<String, Set<String>> incomingEdges = new HashMap<>();

    // Start nodes (nodes with no incoming edges)
    private final Set<String> startNodes = new HashSet<>();

    /**
     * Add a node to the graph.
     *
     * @param nodeId the node ID
     */
    public void addNode(String nodeId) {
        nodes.add(nodeId);
        outgoingEdges.putIfAbsent(nodeId, new HashSet<>());
        incomingEdges.putIfAbsent(nodeId, new HashSet<>());
    }

    /**
     * Add an edge to the graph.
     *
     * @param fromNodeId the source node ID
     * @param toNodeId the target node ID
     */
    public void addEdge(String fromNodeId, String toNodeId) {
        // Ensure nodes exist
        addNode(fromNodeId);
        addNode(toNodeId);

        // Add edge
        outgoingEdges.get(fromNodeId).add(toNodeId);
        incomingEdges.get(toNodeId).add(fromNodeId);

        // If toNode had no incoming edges before, it might have been a start node
        // Now it has an incoming edge, so it's no longer a start node
        startNodes.remove(toNodeId);
    }

    /**
     * Mark a node as a start node.
     *
     * @param nodeId the node ID
     */
    public void markAsStartNode(String nodeId) {
        // Ensure node exists
        addNode(nodeId);

        // Mark as start node
        startNodes.add(nodeId);
    }

    /**
     * Get the outgoing edges for a node.
     *
     * @param nodeId the node ID
     * @return the set of target node IDs
     */
    public Set<String> getOutgoingEdges(String nodeId) {
        return outgoingEdges.getOrDefault(nodeId, Collections.emptySet());
    }

    /**
     * Get the incoming edges for a node.
     *
     * @param nodeId the node ID
     * @return the set of source node IDs
     */
    public Set<String> getIncomingEdges(String nodeId) {
        return incomingEdges.getOrDefault(nodeId, Collections.emptySet());
    }

    /**
     * Get all nodes in the graph.
     *
     * @return the set of node IDs
     */
    public Set<String> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    /**
     * Get the start nodes (nodes with no incoming edges).
     *
     * @return the set of start node IDs
     */
    public Set<String> getStartNodes() {
        // If startNodes is empty but we have nodes, we need to find the start nodes
        if (startNodes.isEmpty() && !nodes.isEmpty()) {
            // Find nodes with no incoming edges
            for (String nodeId : nodes) {
                if (incomingEdges.get(nodeId).isEmpty()) {
                    startNodes.add(nodeId);
                }
            }
        }

        return Collections.unmodifiableSet(startNodes);
    }

    /**
     * Check if the graph contains a node.
     *
     * @param nodeId the node ID
     * @return true if the graph contains the node
     */
    public boolean containsNode(String nodeId) {
        return nodes.contains(nodeId);
    }

    /**
     * Check if the graph contains an edge.
     *
     * @param fromNodeId the source node ID
     * @param toNodeId the target node ID
     * @return true if the graph contains the edge
     */
    public boolean containsEdge(String fromNodeId, String toNodeId) {
        return outgoingEdges.containsKey(fromNodeId) &&
                outgoingEdges.get(fromNodeId).contains(toNodeId);
    }

    /**
     * Check if the graph is empty.
     *
     * @return true if the graph is empty
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /**
     * Get the number of nodes in the graph.
     *
     * @return the number of nodes
     */
    public int size() {
        return nodes.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WorkflowDAG{nodes=").append(nodes.size())
                .append(", edges=");

        int edgeCount = 0;
        for (Set<String> edges : outgoingEdges.values()) {
            edgeCount += edges.size();
        }

        sb.append(edgeCount)
                .append(", startNodes=").append(startNodes)
                .append('}');

        return sb.toString();
    }
}