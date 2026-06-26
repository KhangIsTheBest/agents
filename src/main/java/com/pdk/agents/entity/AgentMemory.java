package com.pdk.agents.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity đại diện cho Bộ nhớ ngữ nghĩa (Semantic Memory) của Agent.
 * Lưu trữ văn bản gốc và vector embedding 768 chiều (pgvector).
 * Dùng cho RAG và Semantic Cache sau này.
 */
@Entity
@Table(name = "agent_memories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Node agent;

    @Column(name = "context_text", nullable = false, columnDefinition = "TEXT")
    private String contextText;

    /**
     * Vector embedding 768 chiều.
     * Được lưu dưới dạng chuỗi trong JPA, nhưng PostgreSQL sẽ dùng kiểu vector(768).
     * Các truy vấn vector sẽ dùng native query.
     */
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private String embedding;
}
