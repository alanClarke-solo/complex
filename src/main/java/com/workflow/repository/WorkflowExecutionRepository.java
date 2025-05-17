package com.workflow.repository;

import com.workflow.model.WorkflowExecution;
import com.workflow.model.WorkflowStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowExecutionRepository extends CrudRepository<WorkflowExecution, UUID> {
    List<WorkflowExecution> findByWorkflowId(String workflowId);
    List<WorkflowExecution> findByStatus(WorkflowStatus status);
    Optional<WorkflowExecution> findByExecutionId(UUID executionId);
}