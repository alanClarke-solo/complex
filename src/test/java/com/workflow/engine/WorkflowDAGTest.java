package com.workflow.engine;

import com.workflow.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowDAGTest {
    private WorkflowDAG dag;
    private Task task1;
    private Task task2;
    private Task task3;
    
    @BeforeEach
    void setUp() {
        dag = new WorkflowDAG();
        
        task1 = new Task();
        task1.setTaskId("task1");
        task1.setName("Task 1");
        
        task2 = new Task();
        task2.setTaskId("task2");
        task2.setName("Task 2");
        
        task3 = new Task();
        task3.setTaskId("task3");
        task3.setName("Task 3");
        
        dag.addNode("task1", task1);
        dag.addNode("task2", task2);
        dag.addNode("task3", task3);
    }
    
    @Test
    void testAddNode() {
        assertEquals(task1, dag.getTask("task1"));
        assertEquals(task2, dag.getTask("task2"));
        assertEquals(task3, dag.getTask("task3"));
    }
    
    @Test
    void testAddEdge() {
        dag.addEdge("task1", "task2");
        dag.addEdge("task1", "task3");
        
        assertEquals(2, dag.getNextTasks("task1").size());
        assertTrue(dag.getNextTasks("task1").contains("task2"));
        assertTrue(dag.getNextTasks("task1").contains("task3"));
    }
    
    @Test
    void testGetStartNodes() {
        dag.addEdge("task1", "task2");
        dag.addEdge("task1", "task3");
        
        Set<String> startNodes = dag.getStartNodes();
        assertEquals(1, startNodes.size());
        assertTrue(startNodes.contains("task1"));
    }
    
    @Test
    void testHasCycle_NoCycle() {
        dag.addEdge("task1", "task2");
        dag.addEdge("task2", "task3");
        
        assertFalse(dag.hasCycle());
    }
    
    @Test
    void testHasCycle_WithCycle() {
        dag.addEdge("task1", "task2");
        dag.addEdge("task2", "task3");
        dag.addEdge("task3", "task1");
        
        assertTrue(dag.hasCycle());
    }
}