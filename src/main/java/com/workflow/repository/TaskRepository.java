package com.workflow.repository;

import com.workflow.model.Task;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends CrudRepository<Task, String> {
    List<Task> findByWorkflowId(String workflowId);
}