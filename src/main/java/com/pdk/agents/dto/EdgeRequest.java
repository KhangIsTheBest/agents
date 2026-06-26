package com.pdk.agents.dto;

/**
 * Request DTO để tạo/cập nhật Edge (kết nối giữa 2 Node).
 */
public record EdgeRequest(
        String workflowId,
        String sourceNodeId,
        String targetNodeId,
        String conditionExpression
) {}
