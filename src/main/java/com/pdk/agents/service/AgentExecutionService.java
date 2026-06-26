package com.pdk.agents.service;

import com.pdk.agents.entity.Node;
import com.pdk.agents.entity.enums.ModelSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

/**
 * Service gọi mô hình AI (Ollama Local & OpenAI Cloud) qua Spring AI ChatClient.
 *
 * Tính năng:
 * - Định tuyến kết hợp (Hybrid Routing): Tự động định tuyến cuộc gọi dựa trên model source của Node.
 * - Cơ chế tự phục hồi (Local Fallback): Chuyển hướng dự phòng sang Ollama Local nếu OpenAI Cloud bị lỗi.
 * - Inject system prompt, conversation history và semantic memory (RAG).
 * - Redis cache để tránh gọi lặp cho cùng input.
 */
@Service
@Slf4j
public class AgentExecutionService {

    private final ChatClient localChatClient;
    private final ChatClient cloudChatClient;
    private final MemoryService memoryService;
    private final CacheService cacheService;

    public AgentExecutionService(OllamaChatModel localModel,
                                 OpenAiChatModel cloudModel,
                                 MemoryService memoryService,
                                 CacheService cacheService) {
        this.localChatClient = ChatClient.builder(localModel).build();
        this.cloudChatClient = ChatClient.builder(cloudModel).build();
        this.memoryService = memoryService;
        this.cacheService = cacheService;
    }

    /**
     * Thực thi Agent đơn giản (không có context/memory).
     */
    public String execute(Node node, String userInput) {
        return execute(node, userInput, "", "");
    }

    /**
     * Thực thi Agent đầy đủ — với conversation history, semantic memory và định tuyến Hybrid.
     *
     * @param node              Node (Agent) cần thực thi
     * @param userInput         Input đầu vào
     * @param conversationHistory Lịch sử hội thoại (formatted string)
     * @param memoryContext     Tri thức liên quan từ semantic memory (RAG)
     * @return Output từ mô hình AI
     */
    public String execute(Node node, String userInput, String conversationHistory, String memoryContext) {
        String systemPrompt = node.getSystemPrompt() != null
                ? node.getSystemPrompt()
                : "You are a helpful AI assistant.";

        log.info("▶ Khởi chạy Agent [{}] (Loại={}, Nguồn cấu hình={})",
                node.getNodeName(), node.getAgentType(), node.getModelSource());

        // 1. Kiểm tra cache trước
        String cached = cacheService.getCachedResponse(node.getId(), systemPrompt, userInput);
        if (cached != null) {
            log.info("⚡ Cache Hit cho Agent [{}]", node.getNodeName());
            return cached;
        }

        // 2. Build enriched prompt với context
        String enrichedInput = buildEnrichedInput(userInput, conversationHistory, memoryContext);
        String response = null;

        try {
            // 3. Định tuyến mô hình (Hybrid Routing) & Cơ chế dự phòng (Local Fallback)
            if (ModelSource.CLOUD.equals(node.getModelSource())) {
                try {
                    log.info("☁ Đang gọi CLOUD LLM (OpenAI) cho Agent [{}]", node.getNodeName());
                    response = cloudChatClient.prompt()
                            .system(systemPrompt)
                            .user(enrichedInput)
                            .call()
                            .content();
                } catch (Exception e) {
                    log.warn("⚠ Gọi CLOUD LLM thất bại cho Agent [{}]: {}. Tự động Fallback về LOCAL Ollama!",
                            node.getNodeName(), e.getMessage());
                    // Fallback sang mô hình local chạy offline
                    response = callLocalModel(systemPrompt, enrichedInput, node.getNodeName());
                }
            } else {
                response = callLocalModel(systemPrompt, enrichedInput, node.getNodeName());
            }

            log.info("✓ Agent [{}] hoàn thành nhiệm vụ. Độ dài kết quả: {} ký tự",
                    node.getNodeName(), response != null ? response.length() : 0);

            // 4. Cache response
            if (response != null) {
                cacheService.cacheResponse(node.getId(), systemPrompt, userInput, response);
            }

            return response;

        } catch (Exception e) {
            log.error("✗ Agent [{}] thất bại hoàn toàn: {}", node.getNodeName(), e.getMessage());
            return "[ERROR] Agent " + node.getNodeName() + " failed: " + e.getMessage();
        }
    }

    /**
     * Helper gọi mô hình Local Ollama.
     */
    private String callLocalModel(String systemPrompt, String enrichedInput, String nodeName) {
        log.info("💻 Đang gọi LOCAL LLM (Ollama - qwen2.5:7b) cho Agent [{}]", nodeName);
        return localChatClient.prompt()
                .system(systemPrompt)
                .user(enrichedInput)
                .call()
                .content();
    }

    /**
     * Build enriched input — gộp user input + conversation history + memory context.
     */
    private String buildEnrichedInput(String userInput, String conversationHistory, String memoryContext) {
        StringBuilder enriched = new StringBuilder();

        // Inject memory context (RAG) nếu có
        if (memoryContext != null && !memoryContext.isBlank()) {
            enriched.append(memoryContext).append("\n");
        }

        // Inject conversation history nếu có
        if (conversationHistory != null && !conversationHistory.isBlank()) {
            enriched.append(conversationHistory).append("\n");
        }

        // User input chính
        enriched.append(userInput);

        return enriched.toString();
    }
}
