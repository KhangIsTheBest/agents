package com.pdk.agents.service;

import com.pdk.agents.entity.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service gọi mô hình AI (Ollama Local) qua Spring AI ChatClient.
 *
 * Tính năng:
 * - Inject system prompt của Agent
 * - Inject conversation history (context)
 * - Inject relevant memories (RAG)
 * - Redis cache để tránh gọi lặp
 * - Multi-model routing (LOCAL/CLOUD) — chuẩn bị cho tương lai
 */
@Service
@Slf4j
public class AgentExecutionService {

    private final ChatClient chatClient;
    private final MemoryService memoryService;
    private final CacheService cacheService;

    public AgentExecutionService(ChatClient.Builder chatClientBuilder,
                                  MemoryService memoryService,
                                  CacheService cacheService) {
        this.chatClient = chatClientBuilder.build();
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
     * Thực thi Agent đầy đủ — với conversation history và semantic memory.
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

        log.info("▶ Executing Agent [{}] (type={}, model={})",
                node.getNodeName(), node.getAgentType(), node.getModelSource());

        // 1. Kiểm tra cache trước
        String cached = cacheService.getCachedResponse(node.getId(), systemPrompt, userInput);
        if (cached != null) {
            return cached;
        }

        // 2. Build enriched prompt với context
        String enrichedInput = buildEnrichedInput(userInput, conversationHistory, memoryContext);

        try {
            // 3. Gọi AI model
            // TODO: Multi-model routing — kiểm tra node.getModelSource()
            //       LOCAL → Ollama ChatClient (hiện tại)
            //       CLOUD → OpenAI ChatClient (cần cấu hình thêm)
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(enrichedInput)
                    .call()
                    .content();

            log.info("✓ Agent [{}] completed. Output length: {} chars",
                    node.getNodeName(), response != null ? response.length() : 0);

            // 4. Cache response
            if (response != null) {
                cacheService.cacheResponse(node.getId(), systemPrompt, userInput, response);
            }

            return response;

        } catch (Exception e) {
            log.error("✗ Agent [{}] failed: {}", node.getNodeName(), e.getMessage());
            return "[ERROR] Agent " + node.getNodeName() + " failed: " + e.getMessage();
        }
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
