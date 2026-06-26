package com.pdk.agents.repository;

import com.pdk.agents.entity.Node;
import com.pdk.agents.entity.enums.AgentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý Node (Agent).
 */
@Repository
public interface NodeRepository extends JpaRepository<Node, String> {

    /**
     * Tìm tất cả Node thuộc một Workflow.
     */
    List<Node> findByWorkflowId(String workflowId);

    /**
     * Tìm tất cả Node theo loại Agent trong một Workflow.
     */
    List<Node> findByWorkflowIdAndAgentType(String workflowId, AgentType agentType);
}
