package com.pdk.agents.controller;

import com.pdk.agents.dto.WorkflowAnalyticsResponse;
import com.pdk.agents.repository.ExecutionStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp dữ liệu thống kê (Analytics) cho hệ thống Multi-Agent.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ExecutionStatRepository statRepository;

    /**
     * Lấy các chỉ số thống kê của một Workflow.
     * GET /api/analytics/workflow/{workflowId}
     */
    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<WorkflowAnalyticsResponse> getWorkflowAnalytics(@PathVariable String workflowId) {
        long totalExecutions = statRepository.countByWorkflowId(workflowId);
        
        if (totalExecutions == 0) {
            return ResponseEntity.ok(new WorkflowAnalyticsResponse(
                    workflowId, 0, 0, 0, 0.0, 0.0, 0, 0.0
            ));
        }

        Long inputSum = statRepository.sumTokensInputByWorkflowId(workflowId);
        Long outputSum = statRepository.sumTokensOutputByWorkflowId(workflowId);
        Double costSum = statRepository.sumCostSavedByWorkflowId(workflowId);
        Double avgDuration = statRepository.avgDurationMsByWorkflowId(workflowId);
        long cacheHits = statRepository.countCacheHitsByWorkflowId(workflowId);

        long totalTokensInput = inputSum != null ? inputSum : 0;
        long totalTokensOutput = outputSum != null ? outputSum : 0;
        double totalCostSaved = costSum != null ? costSum : 0.0;
        double avgDurationMs = avgDuration != null ? avgDuration : 0.0;
        double cacheHitRate = ((double) cacheHits / totalExecutions) * 100.0;

        // Tròn số tiền tiết kiệm đến 4 chữ số thập phân
        totalCostSaved = Math.round(totalCostSaved * 10000.0) / 10000.0;
        avgDurationMs = Math.round(avgDurationMs * 100.0) / 100.0;
        cacheHitRate = Math.round(cacheHitRate * 100.0) / 100.0;

        return ResponseEntity.ok(new WorkflowAnalyticsResponse(
                workflowId,
                totalExecutions,
                totalTokensInput,
                totalTokensOutput,
                totalCostSaved,
                avgDurationMs,
                cacheHits,
                cacheHitRate
        ));
    }
}
