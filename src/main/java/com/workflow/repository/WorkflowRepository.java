package com.workflow.repository;

import com.workflow.model.Workflow;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRepository extends CrudRepository<Workflow, String> {
}