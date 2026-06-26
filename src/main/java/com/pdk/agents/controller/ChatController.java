package com.pdk.agents.controller;

import com.pdk.agents.dto.ChatRequest;
import com.pdk.agents.dto.ChatResponse;
import com.pdk.agents.service.WorkflowExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller — Endpoint chính để chạy Workflow (gọi chuỗi Multi-Agent).
 *
 * POST /api/chat — Gửi message vào Workflow → chạy toàn bộ chuỗi Agent → trả kết quả.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final WorkflowExecutorService workflowExecutorService;

    /**
     * Chạy Workflow end-to-end.
     *
     * Request body:
     * {
     *   "workflowId": "...",
     *   "message": "Nội dung cần xử lý"
     * }
     *
     * Response:
     * {
     *   "workflowId": "...",
     *   "finalOutput": "Kết quả cuối cùng từ Agent cuối",
     *   "executionLog": [ ... từng bước Agent xử lý ... ]
     * }
     */
    @PostMapping
    public ResponseEntity<ChatResponse> executeWorkflow(@RequestBody ChatRequest request) {
        ChatResponse response = workflowExecutorService.execute(request);
        return ResponseEntity.ok(response);
    }
}
