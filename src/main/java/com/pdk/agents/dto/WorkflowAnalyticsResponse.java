package com.pdk.agents.dto;

/**
 * DTO trả về số liệu thống kê (Analytics) cho một Workflow.
 */
public record WorkflowAnalyticsResponse(
        String workflowId,
        long totalExecutions,
        long totalTokensInput,
        long totalTokensOutput,
        double totalCostSaved,
        double avgDurationMs,
        long cacheHits,
        double cacheHitRate // Tỉ lệ phần trăm cache hit
) {}
