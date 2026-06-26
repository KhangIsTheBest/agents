package com.pdk.agents.repository;

import com.pdk.agents.entity.AgentMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý AgentMemory (Bộ nhớ ngữ nghĩa).
 * Bao gồm native query tìm kiếm vector qua pgvector.
 */
@Repository
public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {

    /**
     * Tìm tất cả memory của một Agent.
     */
    List<AgentMemory> findByAgentId(String agentId);

    /**
     * Tìm kiếm ngữ nghĩa (Semantic Search) — tìm các memory gần nhất
     * dựa trên cosine similarity với vector đầu vào.
     *
     * @param agentId   ID của Agent
     * @param embedding Vector embedding dạng chuỗi (e.g., "[0.1, 0.2, ...]")
     * @param limit     Số kết quả trả về
     * @return Danh sách AgentMemory gần nhất
     */
    @Query(value = """
            SELECT am.* FROM agent_memories am
            WHERE am.agent_id = :agentId
            ORDER BY am.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<AgentMemory> findNearestMemories(
            @Param("agentId") String agentId,
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    /**
     * Tìm kiếm ngữ nghĩa toàn cục (không lọc theo Agent).
     */
    @Query(value = """
            SELECT am.* FROM agent_memories am
            ORDER BY am.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<AgentMemory> findNearestMemoriesGlobal(
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );
}
