package com.pdk.agents.entity;

import com.pdk.agents.entity.enums.AgentType;
import com.pdk.agents.entity.enums.ModelSource;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Entity đại diện cho một Node (Agent/Tool) trong Workflow Graph.
 * Mỗi Node có loại Agent, system prompt, và nguồn mô hình AI.
 */
@Entity
@Table(name = "nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Node {

    @Id
    @Column(length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @Column(name = "node_name", nullable = false, length = 100)
    private String nodeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 50)
    private AgentType agentType;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_source", nullable = false, length = 20)
    private ModelSource modelSource;

    @OneToMany(mappedBy = "sourceNode", cascade = CascadeType.ALL)
    private List<Edge> outgoingEdges;

    @OneToMany(mappedBy = "targetNode", cascade = CascadeType.ALL)
    private List<Edge> incomingEdges;

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgentMemory> memories;
}
