package com.pdk.agents.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity đại diện cho một Edge (kết nối) giữa hai Node trong Workflow Graph.
 * Có thể kèm theo điều kiện kích hoạt (condition_expression).
 */
@Entity
@Table(name = "edges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Edge {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id")
    private Node sourceNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id")
    private Node targetNode;

    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String conditionExpression;
}
