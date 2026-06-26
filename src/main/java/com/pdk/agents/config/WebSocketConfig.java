package com.pdk.agents.config;

import com.pdk.agents.websocket.AgentWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Cấu hình WebSockets trong Spring Boot.
 * Kích hoạt WebSocket và ánh xạ đường dẫn /ws/execution.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AgentWebSocketHandler agentWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(agentWebSocketHandler, "/ws/execution")
                .setAllowedOrigins("*"); // Cho phép mọi nguồn gốc truy cập (CORS) cho môi trường local
    }
}
