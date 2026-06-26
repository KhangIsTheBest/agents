package com.pdk.agents.entity.enums;

/**
 * Vai trò của tin nhắn trong hội thoại.
 */
public enum MessageRole {
    USER,       // Tin nhắn từ người dùng
    ASSISTANT,  // Tin nhắn từ Agent AI
    SYSTEM      // Tin nhắn hệ thống (system prompt, context injection)
}
