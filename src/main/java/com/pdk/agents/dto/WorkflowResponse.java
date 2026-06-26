package com.pdk.agents.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO trả về thông tin Workflow (bao gồm danh sách Nodes & Edges).
 */
public record WorkflowResponse(
        String id,
        String name,
        String description,
        LocalDateTime createdAt,
        List<NodeResponse> nodes,
        List<EdgeResponse> edges
) {}
