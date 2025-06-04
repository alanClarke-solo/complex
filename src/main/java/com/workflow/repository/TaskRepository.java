package com.workflow.repository;

import com.workflow.model.Task;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends CrudRepository<Task, String> {
    /**
     * Find tasks by workflow ID.
     *
     * @param workflowId the workflow ID
     * @return the list of tasks
     */
    List<Task> findByWorkflowId(String workflowId);
}