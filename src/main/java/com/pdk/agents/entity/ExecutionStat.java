package com.pdk.agents.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity lưu chỉ số hiệu năng và tối ưu hóa chi phí (Analytics).
 */
@Entity
@Table(name = "execution_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false, length = 50)
    private String workflowId;

    @Column(name = "conversation_id", nullable = false, length = 50)
    private String conversationId;

    @Column(name = "node_name", nullable = false, length = 100)
    private String nodeName;

    @Column(name = "model_source", nullable = false, length = 20)
    private String modelSource;

    @Column(name = "tokens_input", nullable = false)
    private Integer tokensInput;

    @Column(name = "tokens_output", nullable = false)
    private Integer tokensOutput;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "cache_hit", nullable = false)
    private Boolean cacheHit;

    @Column(name = "cost_saved", nullable = false)
    private Double costSaved;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
