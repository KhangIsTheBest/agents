package com.pdk.agents.repository;

import com.pdk.agents.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository quản lý Workflow.
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, String> {
}
