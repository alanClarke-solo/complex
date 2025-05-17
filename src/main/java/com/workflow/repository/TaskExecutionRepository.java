package com.workflow.repository;

import com.workflow.model.TaskExecution;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskExecutionRepository extends CrudRepository<TaskExecution, UUID> {
    List<TaskExecution> findByWorkflowExecutionId(UUID workflowExecutionId);
}