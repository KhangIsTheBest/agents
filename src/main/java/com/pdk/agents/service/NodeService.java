package com.pdk.agents.service;

import com.pdk.agents.dto.NodeRequest;
import com.pdk.agents.dto.NodeResponse;
import com.pdk.agents.entity.Node;
import com.pdk.agents.entity.Workflow;
import com.pdk.agents.entity.enums.AgentType;
import com.pdk.agents.exception.ResourceNotFoundException;
import com.pdk.agents.repository.NodeRepository;
import com.pdk.agents.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý CRUD cho Node (Agent).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NodeService {

    private final NodeRepository nodeRepository;
    private final WorkflowRepository workflowRepository;

    /**
     * Tạo Node mới trong Workflow.
     */
    public NodeResponse create(NodeRequest request) {
        Workflow workflow = workflowRepository.findById(request.workflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", request.workflowId()));

        Node node = Node.builder()
                .id(UUID.randomUUID().toString())
                .workflow(workflow)
                .nodeName(request.nodeName())
                .agentType(request.agentType())
                .systemPrompt(request.systemPrompt())
                .modelSource(request.modelSource())
                .build();

        Node saved = nodeRepository.save(node);
        return toResponse(saved);
    }

    /**
     * Lấy tất cả Node trong một Workflow.
     */
    @Transactional(readOnly = true)
    public List<NodeResponse> findByWorkflowId(String workflowId) {
        return nodeRepository.findByWorkflowId(workflowId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy tất cả Node theo loại Agent trong Workflow.
     */
    @Transactional(readOnly = true)
    public List<NodeResponse> findByWorkflowIdAndType(String workflowId, AgentType agentType) {
        return nodeRepository.findByWorkflowIdAndAgentType(workflowId, agentType).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy Node theo ID.
     */
    @Transactional(readOnly = true)
    public NodeResponse findById(String id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Node", "id", id));
        return toResponse(node);
    }

    /**
     * Cập nhật Node.
     */
    public NodeResponse update(String id, NodeRequest request) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Node", "id", id));

        node.setNodeName(request.nodeName());
        node.setAgentType(request.agentType());
        node.setSystemPrompt(request.systemPrompt());
        node.setModelSource(request.modelSource());

        Node saved = nodeRepository.save(node);
        return toResponse(saved);
    }

    /**
     * Xóa Node.
     */
    public void delete(String id) {
        if (!nodeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Node", "id", id);
        }
        nodeRepository.deleteById(id);
    }

    // ===== Mapper =====

    private NodeResponse toResponse(Node node) {
        return new NodeResponse(
                node.getId(),
                node.getWorkflow().getId(),
                node.getNodeName(),
                node.getAgentType(),
                node.getSystemPrompt(),
                node.getModelSource()
        );
    }
}
