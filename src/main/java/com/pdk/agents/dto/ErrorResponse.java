package com.pdk.agents.dto;

import java.time.LocalDateTime;

/**
 * Response chuẩn cho lỗi API — trả JSON thay vì stack trace.
 */
public record ErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp
) {
    public ErrorResponse(int status, String message) {
        this(status, message, LocalDateTime.now());
    }
}
