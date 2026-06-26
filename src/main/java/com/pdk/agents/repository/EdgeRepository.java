package com.pdk.agents.repository;

import com.pdk.agents.entity.Edge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý Edge (kết nối giữa các Agent).
 */
@Repository
public interface EdgeRepository extends JpaRepository<Edge, String> {

    /**
     * Tìm tất cả Edge thuộc một Workflow.
     */
    List<Edge> findByWorkflowId(String workflowId);

    /**
     * Tìm tất cả Edge xuất phát từ một Node.
     */
    List<Edge> findBySourceNodeId(String sourceNodeId);

    /**
     * Tìm tất cả Edge đến một Node.
     */
    List<Edge> findByTargetNodeId(String targetNodeId);
}
