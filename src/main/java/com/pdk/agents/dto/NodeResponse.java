package com.pdk.agents.dto;

import com.pdk.agents.entity.enums.AgentType;
import com.pdk.agents.entity.enums.ModelSource;

/**
 * Response DTO trả về thông tin Node (Agent).
 */
public record NodeResponse(
        String id,
        String workflowId,
        String nodeName,
        AgentType agentType,
        String systemPrompt,
        ModelSource modelSource
) {}
