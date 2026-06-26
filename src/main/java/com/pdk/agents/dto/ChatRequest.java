package com.pdk.agents.dto;

/**
 * Request DTO để chạy Workflow — gửi tin nhắn vào hệ thống Multi-Agent.
 * conversationId = null → tạo phiên hội thoại mới.
 */
public record ChatRequest(
        String workflowId,
        String message,
        String conversationId  // null = phiên mới
) {}
