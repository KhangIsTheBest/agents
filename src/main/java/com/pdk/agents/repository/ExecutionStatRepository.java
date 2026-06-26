package com.pdk.agents.repository;

import com.pdk.agents.entity.ExecutionStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository quản lý dữ liệu thống kê hiệu năng (ExecutionStat).
 */
public interface ExecutionStatRepository extends JpaRepository<ExecutionStat, Long> {

    List<ExecutionStat> findByWorkflowId(String workflowId);

    @Query("SELECT COUNT(e) FROM ExecutionStat e WHERE e.workflowId = :workflowId")
    long countByWorkflowId(@Param("workflowId") String workflowId);

    @Query("SELECT SUM(e.tokensInput) FROM ExecutionStat e WHERE e.workflowId = :workflowId")
    Long sumTokensInputByWorkflowId(@Param("workflowId") String workflowId);

    @Query("SELECT SUM(e.tokensOutput) FROM ExecutionStat e WHERE e.workflowId = :workflowId")
    Long sumTokensOutputByWorkflowId(@Param("workflowId") String workflowId);

    @Query("SELECT SUM(e.costSaved) FROM ExecutionStat e WHERE e.workflowId = :workflowId")
    Double sumCostSavedByWorkflowId(@Param("workflowId") String workflowId);

    @Query("SELECT AVG(e.durationMs) FROM ExecutionStat e WHERE e.workflowId = :workflowId")
    Double avgDurationMsByWorkflowId(@Param("workflowId") String workflowId);

    @Query("SELECT COUNT(e) FROM ExecutionStat e WHERE e.workflowId = :workflowId AND e.cacheHit = true")
    long countCacheHitsByWorkflowId(@Param("workflowId") String workflowId);
}
