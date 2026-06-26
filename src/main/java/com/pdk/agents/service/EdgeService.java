package com.pdk.agents.service;

import com.pdk.agents.dto.EdgeRequest;
import com.pdk.agents.dto.EdgeResponse;
import com.pdk.agents.entity.Edge;
import com.pdk.agents.entity.Node;
import com.pdk.agents.entity.Workflow;
import com.pdk.agents.exception.ResourceNotFoundException;
import com.pdk.agents.repository.EdgeRepository;
import com.pdk.agents.repository.NodeRepository;
import com.pdk.agents.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý CRUD cho Edge (kết nối giữa các Agent).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EdgeService {

    private final EdgeRepository edgeRepository;
    private final WorkflowRepository workflowRepository;
    private final NodeRepository nodeRepository;

    /**
     * Tạo Edge mới — nối 2 Node trong Workflow.
     */
    public EdgeResponse create(EdgeRequest request) {
        Workflow workflow = workflowRepository.findById(request.workflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", request.workflowId()));

        Node sourceNode = nodeRepository.findById(request.sourceNodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Source Node", "id", request.sourceNodeId()));

        Node targetNode = nodeRepository.findById(request.targetNodeId())
                .orElseThrow(() -> new ResourceNotFoundException("Target Node", "id", request.targetNodeId()));

        Edge edge = Edge.builder()
                .id(UUID.randomUUID().toString())
                .workflow(workflow)
                .sourceNode(sourceNode)
                .targetNode(targetNode)
                .conditionExpression(request.conditionExpression())
                .build();

        Edge saved = edgeRepository.save(edge);
        return toResponse(saved);
    }

    /**
     * Lấy tất cả Edge trong một Workflow.
     */
    @Transactional(readOnly = true)
    public List<EdgeResponse> findByWorkflowId(String workflowId) {
        return edgeRepository.findByWorkflowId(workflowId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy Edge theo ID.
     */
    @Transactional(readOnly = true)
    public EdgeResponse findById(String id) {
        Edge edge = edgeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Edge", "id", id));
        return toResponse(edge);
    }

    /**
     * Xóa Edge.
     */
    public void delete(String id) {
        if (!edgeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Edge", "id", id);
        }
        edgeRepository.deleteById(id);
    }

    // ===== Mapper =====

    private EdgeResponse toResponse(Edge edge) {
        return new EdgeResponse(
                edge.getId(),
                edge.getWorkflow().getId(),
                edge.getSourceNode().getId(),
                edge.getTargetNode().getId(),
                edge.getConditionExpression()
        );
    }
}
