package com.pdk.agents.dto;

/**
 * Request DTO để tạo/cập nhật Workflow.
 */
public record WorkflowRequest(
        String name,
        String description
) {}
