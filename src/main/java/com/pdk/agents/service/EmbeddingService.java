package com.pdk.agents.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service chuyển đổi văn bản thành vector embedding qua Ollama (nomic-embed-text).
 * Vector 768 chiều dùng cho Semantic Memory và RAG.
 */
@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Embed một đoạn văn bản thành vector float[].
     *
     * @param text Văn bản cần embed
     * @return Mảng float 768 chiều
     */
    public float[] embed(String text) {
        try {
            EmbeddingResponse response = embeddingModel.call(
                    new org.springframework.ai.embedding.EmbeddingRequest(
                            List.of(text), null));

            float[] embedding = response.getResult().getOutput();
            log.debug("Embedded text ({} chars) → vector[{}]", text.length(), embedding.length);
            return embedding;

        } catch (Exception e) {
            log.error("Embedding failed for text: {}...", text.substring(0, Math.min(50, text.length())), e);
            throw new RuntimeException("Embedding service error: " + e.getMessage(), e);
        }
    }

    /**
     * Chuyển float[] thành chuỗi pgvector format: "[0.1,0.2,0.3,...]"
     */
    public String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
