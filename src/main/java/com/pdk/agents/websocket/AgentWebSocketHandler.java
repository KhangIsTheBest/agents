package com.pdk.agents.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdk.agents.dto.WebSocketEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler quản lý kết nối và gửi thông điệp WebSockets.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.info("🔌 Thiết lập kết nối WebSocket thành công: Session ID [{}]", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("🔌 Ngắt kết nối WebSocket: Session ID [{}], Status: {}", session.getId(), status);
    }

    /**
     * Gửi sự kiện đến toàn bộ các Client đang kết nối.
     */
    public void broadcastEvent(WebSocketEvent event) {
        if (sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("❌ Lỗi serialize thông điệp WebSocket: {}", e.getMessage());
            return;
        }

        TextMessage textMessage = new TextMessage(json);
        log.debug("📡 Broadcasting WebSocket event [{}]", event.type());

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("❌ Không thể gửi tin nhắn đến session [{}]: {}", session.getId(), e.getMessage());
                }
            }
        }
    }
}
