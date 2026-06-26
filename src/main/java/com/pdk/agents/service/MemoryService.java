package com.pdk.agents.service;

import com.pdk.agents.entity.AgentMemory;
import com.pdk.agents.entity.Node;
import com.pdk.agents.repository.AgentMemoryRepository;
import com.pdk.agents.repository.NodeRepository;
import com.pdk.agents.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản lý Semantic Memory (Bộ nhớ ngữ nghĩa).
 * Lưu trữ tri thức dạng vector (pgvector) và tìm kiếm ngữ nghĩa cho RAG.
 *
 * Flow:
 * 1. Agent xử lý xong → lưu output vào memory (embed → pgvector)
 * 2. Agent nhận input mới → tìm memory liên quan → inject vào prompt
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemoryService {

    private final AgentMemoryRepository memoryRepository;
    private final NodeRepository nodeRepository;
    private final EmbeddingService embeddingService;

    @Value("${app.memory.search-limit:5}")
    private int defaultSearchLimit;

    /**
     * Lưu tri thức mới vào bộ nhớ của Agent.
     *
     * @param agentId ID của Agent (Node)
     * @param text    Văn bản cần lưu
     */
    public void store(String agentId, String text) {
        Node agent = nodeRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Node", "id", agentId));

        // Embed text thành vector
        float[] embedding = embeddingService.embed(text);
        String vectorString = embeddingService.toVectorString(embedding);

        AgentMemory memory = AgentMemory.builder()
                .agent(agent)
                .contextText(text)
                .embedding(vectorString)
                .build();

        memoryRepository.save(memory);
        log.info("💾 Stored memory for Agent [{}]: {} chars → vector[{}]",
                agent.getNodeName(), text.length(), embedding.length);
    }

    /**
     * Tìm kiếm ngữ nghĩa — lấy các memory liên quan nhất cho Agent.
     *
     * @param agentId ID của Agent
     * @param query   Câu truy vấn (sẽ được embed để so sánh cosine)
     * @return Danh sách văn bản memory liên quan
     */
    @Transactional(readOnly = true)
    public List<String> searchRelevant(String agentId, String query) {
        return searchRelevant(agentId, query, defaultSearchLimit);
    }

    /**
     * Tìm kiếm ngữ nghĩa với giới hạn tùy chỉnh.
     */
    @Transactional(readOnly = true)
    public List<String> searchRelevant(String agentId, String query, int limit) {
        float[] queryEmbedding = embeddingService.embed(query);
        String vectorString = embeddingService.toVectorString(queryEmbedding);

        List<AgentMemory> memories = memoryRepository.findNearestMemories(agentId, vectorString, limit);

        log.info("🔍 Semantic search for Agent [{}]: found {} relevant memories",
                agentId, memories.size());

        return memories.stream()
                .map(AgentMemory::getContextText)
                .collect(Collectors.toList());
    }

    /**
     * Tìm kiếm toàn cục (không lọc theo Agent).
     */
    @Transactional(readOnly = true)
    public List<String> searchGlobal(String query, int limit) {
        float[] queryEmbedding = embeddingService.embed(query);
        String vectorString = embeddingService.toVectorString(queryEmbedding);

        List<AgentMemory> memories = memoryRepository.findNearestMemoriesGlobal(vectorString, limit);

        return memories.stream()
                .map(AgentMemory::getContextText)
                .collect(Collectors.toList());
    }

    /**
     * Format danh sách memory thành chuỗi context để inject vào prompt.
     */
    public String formatAsContext(List<String> memories) {
        if (memories == null || memories.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("=== RELEVANT KNOWLEDGE ===\n");
        for (int i = 0; i < memories.size(); i++) {
            context.append(String.format("[%d] %s\n", i + 1, memories.get(i)));
        }
        context.append("=== END KNOWLEDGE ===\n");

        return context.toString();
    }
}
