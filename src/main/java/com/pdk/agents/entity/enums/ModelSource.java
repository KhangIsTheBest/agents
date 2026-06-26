package com.pdk.agents.entity.enums;

/**
 * Nguồn mô hình AI mà Agent sử dụng.
 */
public enum ModelSource {
    LOCAL,  // Mô hình chạy Local qua Ollama (e.g., qwen2.5:7b)
    CLOUD   // Mô hình Cloud (e.g., OpenAI GPT-4)
}
