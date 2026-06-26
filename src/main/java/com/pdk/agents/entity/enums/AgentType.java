package com.pdk.agents.entity.enums;

/**
 * Loại Agent trong hệ thống Multi-Agent.
 */
public enum AgentType {
    ROUTER,     // Agent định tuyến — phân tích và chuyển hướng yêu cầu
    EXECUTOR,   // Agent thực thi — xử lý tác vụ chính
    VALIDATOR   // Agent kiểm tra — xác minh kết quả đầu ra
}
