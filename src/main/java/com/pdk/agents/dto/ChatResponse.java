package com.pdk.agents.dto;

import java.util.List;

/**
 * Response DTO trả về kết quả chạy Workflow.
 * Bao gồm conversationId để tiếp tục hội thoại, output cuối cùng và log từng bước.
 */
public record ChatResponse(
        String workflowId,
        String conversationId,
        String finalOutput,
        List<ExecutionStep> executionLog
) {}
