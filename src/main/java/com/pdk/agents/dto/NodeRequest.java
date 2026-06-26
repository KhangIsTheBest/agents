package com.pdk.agents.dto;

import com.pdk.agents.entity.enums.AgentType;
import com.pdk.agents.entity.enums.ModelSource;

/**
 * Request DTO để tạo/cập nhật Node (Agent).
 */
public record NodeRequest(
        String workflowId,
        String nodeName,
        AgentType agentType,
        String systemPrompt,
        ModelSource modelSource
) {}
