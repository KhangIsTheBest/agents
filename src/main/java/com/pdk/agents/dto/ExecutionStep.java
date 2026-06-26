package com.pdk.agents.dto;

/**
 * DTO ghi lại một bước thực thi trong Workflow.
 * Mỗi step tương ứng với một Node (Agent) được kích hoạt.
 */
public record ExecutionStep(
        int stepOrder,
        String nodeId,
        String nodeName,
        String agentType,
        String input,
        String output
) {}
