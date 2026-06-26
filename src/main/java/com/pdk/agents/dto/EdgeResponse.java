package com.pdk.agents.dto;

/**
 * Response DTO trả về thông tin Edge.
 */
public record EdgeResponse(
        String id,
        String workflowId,
        String sourceNodeId,
        String targetNodeId,
        String conditionExpression
) {}
