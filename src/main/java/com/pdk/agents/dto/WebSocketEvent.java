package com.pdk.agents.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Event DTO gửi thông tin qua WebSocket.
 * Dùng để đồng bộ trạng thái luồng chạy của Multi-Agent thời gian thực.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebSocketEvent(
        String type,           // "NODE_RUNNING", "NODE_COMPLETED", "WORKFLOW_COMPLETED", "WORKFLOW_FAILED"
        String workflowId,
        String conversationId,
        String nodeId,
        String nodeName,
        String agentType,
        String status,         // "RUNNING", "COMPLETED", "FAILED"
        ExecutionStep step,
        String payload         // chứa finalOutput hoặc errorMessage
) {}
