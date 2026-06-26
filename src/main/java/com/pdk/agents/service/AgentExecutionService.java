package com.pdk.agents.service;

import com.pdk.agents.entity.Node;
import com.pdk.agents.entity.ExecutionStat;
import com.pdk.agents.entity.enums.ModelSource;
import com.pdk.agents.repository.ExecutionStatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service gọi mô hình AI (Ollama Local & OpenAI Cloud) qua Spring AI ChatClient.
 *
 * Tích hợp:
 * - Bảo mật dữ liệu (PII Redaction): Tự động che mờ thông tin cá nhân trước khi gửi lên Cloud và hoàn tác sau khi nhận kết quả.
 * - Đo lường chỉ số (Analytics): Ghi nhận token, thời gian xử lý và số tiền tiết kiệm được.
 */
@Service
@Slf4j
public class AgentExecutionService {

    private final ChatClient localChatClient;
    private final ChatClient cloudChatClient;
    private final MemoryService memoryService;
    private final CacheService cacheService;
    private final PrivacyFilterService privacyFilterService;
    private final ExecutionStatRepository statRepository;

    public AgentExecutionService(OllamaChatModel localModel,
                                 OpenAiChatModel cloudModel,
                                 MemoryService memoryService,
                                 CacheService cacheService,
                                 PrivacyFilterService privacyFilterService,
                                 ExecutionStatRepository statRepository) {
        this.localChatClient = ChatClient.builder(localModel).build();
        this.cloudChatClient = ChatClient.builder(cloudModel).build();
        this.memoryService = memoryService;
        this.cacheService = cacheService;
        this.privacyFilterService = privacyFilterService;
        this.statRepository = statRepository;
    }

    /**
     * Thực thi Agent đơn giản (không có context/memory).
     */
    public String execute(Node node, String userInput) {
        return execute(node, userInput, "system-test", "", "");
    }

    /**
     * Thực thi Agent đầy đủ — với conversation history, semantic memory, định tuyến Hybrid và bảo mật PII.
     */
    public String execute(Node node, String userInput, String conversationId, String conversationHistory, String memoryContext) {
        String systemPrompt = node.getSystemPrompt() != null
                ? node.getSystemPrompt()
                : "You are a helpful AI assistant.";

        log.info("▶ Khởi chạy Agent [{}] (Loại={}, Nguồn cấu hình={})",
                node.getNodeName(), node.getAgentType(), node.getModelSource());

        long startTime = System.currentTimeMillis();

        // 1. Kiểm tra cache trước
        String cached = cacheService.getCachedResponse(node.getId(), systemPrompt, userInput);
        if (cached != null) {
            log.info("⚡ Cache Hit cho Agent [{}]", node.getNodeName());
            
            // Ghi nhận thống kê Cache Hit
            saveStat(node.getWorkflow().getId(), conversationId, node.getNodeName(), node.getModelSource().name(), 
                     0, 0, 0L, true, 0.005); // Cache hit tiết kiệm cố định $0.005
                     
            return cached;
        }

        // 2. Build enriched prompt với context
        String enrichedInput = buildEnrichedInput(userInput, conversationHistory, memoryContext);
        
        CallResult result;
        boolean isFallbackUsed = false;

        try {
            // 3. Định tuyến mô hình (Hybrid Routing) & Cơ chế dự phòng (Local Fallback)
            if (ModelSource.CLOUD.equals(node.getModelSource())) {
                // Che mờ dữ liệu PII nhạy cảm trước khi gửi đi Cloud
                PrivacyFilterService.FilterResult redaction = privacyFilterService.anonymize(enrichedInput);
                String cleanInput = redaction.cleanText();
                
                if (!redaction.replacements().isEmpty()) {
                    log.info("🔒 [Bảo mật] Đã che mờ {} thông tin PII nhạy cảm trước khi gửi lên Cloud.", 
                            redaction.replacements().size());
                }

                try {
                    log.info("☁ Đang gọi CLOUD LLM (OpenAI) cho Agent [{}]", node.getNodeName());
                    result = callModel(cloudChatClient, systemPrompt, cleanInput);
                    
                    // Phục hồi lại dữ liệu PII ban đầu trong phản hồi của AI
                    String restoredText = privacyFilterService.deAnonymize(result.content(), redaction.replacements());
                    result = new CallResult(restoredText, result.inputTokens(), result.outputTokens(), result.durationMs());
                    
                } catch (Exception e) {
                    log.warn("⚠ Gọi CLOUD LLM thất bại cho Agent [{}]: {}. Tự động Fallback về LOCAL Ollama!",
                            node.getNodeName(), e.getMessage());
                    isFallbackUsed = true;
                    // Phục hồi lại dữ liệu trước khi gọi Local (chạy offline nội bộ nên không cần che mờ)
                    result = callModel(localChatClient, systemPrompt, enrichedInput);
                }
            } else {
                result = callModel(localChatClient, systemPrompt, enrichedInput);
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("✓ Agent [{}] hoàn thành nhiệm vụ. Thời gian phản hồi: {}ms, Lượng Token (In/Out): {}/{}",
                    node.getNodeName(), totalDuration, result.inputTokens(), result.outputTokens());

            // 4. Tính toán số tiền tiết kiệm được (USD)
            double costSaved = 0.0;
            if (ModelSource.LOCAL.equals(node.getModelSource()) || isFallbackUsed) {
                // Chạy Local tránh được chi phí Cloud (Tính theo giá GPT-4o-mini: 0.15$/1M In, 0.60$/1M Out)
                costSaved = (result.inputTokens() * 0.15 + result.outputTokens() * 0.60) / 1_000_000.0;
            }

            // 5. Ghi nhận thống kê vào DB
            saveStat(node.getWorkflow().getId(), conversationId, node.getNodeName(), 
                     isFallbackUsed ? "LOCAL (FALLBACK)" : node.getModelSource().name(), 
                     result.inputTokens(), result.outputTokens(), totalDuration, false, costSaved);

            // 6. Cache response
            if (result.content() != null) {
                cacheService.cacheResponse(node.getId(), systemPrompt, userInput, result.content());
            }

            return result.content();

        } catch (Exception e) {
            log.error("✗ Agent [{}] thất bại hoàn toàn: {}", node.getNodeName(), e.getMessage());
            saveStat(node.getWorkflow().getId(), conversationId, node.getNodeName(), node.getModelSource().name(), 
                     0, 0, System.currentTimeMillis() - startTime, false, 0.0);
            return "[ERROR] Agent " + node.getNodeName() + " failed: " + e.getMessage();
        }
    }

    /**
     * Gọi mô hình thông qua ChatClient và lấy Metadata Token Usage.
     */
    private CallResult callModel(ChatClient client, String systemPrompt, String enrichedInput) {
        long start = System.currentTimeMillis();
        org.springframework.ai.chat.model.ChatResponse chatResponse = client.prompt()
                .system(systemPrompt)
                .user(enrichedInput)
                .call()
                .chatResponse();
        long duration = System.currentTimeMillis() - start;

        String content = "";
        if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
            content = chatResponse.getResult().getOutput().getText();
        }

        int inputTokens = 0;
        int outputTokens = 0;

        // Trích xuất lượng token từ Spring AI Metadata Usage
        var usage = chatResponse.getMetadata().getUsage();
        if (usage != null) {
            if (usage.getPromptTokens() != null) inputTokens = usage.getPromptTokens().intValue();
            if (usage.getCompletionTokens() != null) outputTokens = usage.getCompletionTokens().intValue();
        }

        // Dự phòng nếu metadata trống (Ollama local đôi khi không trả về token)
        if (inputTokens == 0) inputTokens = enrichedInput.length() / 4;
        if (outputTokens == 0) outputTokens = content.length() / 4;

        return new CallResult(content, inputTokens, outputTokens, duration);
    }

    /**
     * Lưu dữ liệu thống kê.
     */
    private void saveStat(String workflowId, String conversationId, String nodeName, String modelSource,
                          int inputTokens, int outputTokens, long durationMs, boolean cacheHit, double costSaved) {
        try {
            ExecutionStat stat = ExecutionStat.builder()
                    .workflowId(workflowId)
                    .conversationId(conversationId)
                    .nodeName(nodeName)
                    .modelSource(modelSource)
                    .tokensInput(inputTokens)
                    .tokensOutput(outputTokens)
                    .durationMs(durationMs)
                    .cacheHit(cacheHit)
                    .costSaved(costSaved)
                    .build();
            statRepository.save(stat);
        } catch (Exception e) {
            log.error("❌ Không thể ghi nhận chỉ số thống kê (ExecutionStat): {}", e.getMessage());
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

    private record CallResult(String content, int inputTokens, int outputTokens, long durationMs) {}
}
