package com.pdk.agents.service;

import com.pdk.agents.dto.*;
import com.pdk.agents.entity.Workflow;
import com.pdk.agents.exception.ResourceNotFoundException;
import com.pdk.agents.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service quản lý CRUD cho Workflow.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowService {

    private final WorkflowRepository workflowRepository;

    /**
     * Tạo Workflow mới.
     */
    public WorkflowResponse create(WorkflowRequest request) {
        Workflow workflow = Workflow.builder()
                .id(UUID.randomUUID().toString())
                .name(request.name())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .build();

        Workflow saved = workflowRepository.save(workflow);
        return toResponse(saved);
    }

    /**
     * Lấy tất cả Workflow.
     */
    @Transactional(readOnly = true)
    public List<WorkflowResponse> findAll() {
        return workflowRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy Workflow theo ID.
     */
    @Transactional(readOnly = true)
    public WorkflowResponse findById(String id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));
        return toDetailedResponse(workflow);
    }

    /**
     * Cập nhật Workflow.
     */
    public WorkflowResponse update(String id, WorkflowRequest request) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));

        workflow.setName(request.name());
        workflow.setDescription(request.description());

        Workflow saved = workflowRepository.save(workflow);
        return toResponse(saved);
    }

    /**
     * Xóa Workflow (cascade xóa tất cả Nodes & Edges).
     */
    public void delete(String id) {
        if (!workflowRepository.existsById(id)) {
            throw new ResourceNotFoundException("Workflow", "id", id);
        }
        workflowRepository.deleteById(id);
    }

    // ===== Mapper Methods =====

    private WorkflowResponse toResponse(Workflow workflow) {
        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getCreatedAt(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private WorkflowResponse toDetailedResponse(Workflow workflow) {
        List<NodeResponse> nodes = workflow.getNodes() != null
                ? workflow.getNodes().stream()
                    .map(n -> new NodeResponse(
                            n.getId(), workflow.getId(), n.getNodeName(),
                            n.getAgentType(), n.getSystemPrompt(), n.getModelSource()))
                    .toList()
                : Collections.emptyList();

        List<EdgeResponse> edges = workflow.getEdges() != null
                ? workflow.getEdges().stream()
                    .map(e -> new EdgeResponse(
                            e.getId(), workflow.getId(),
                            e.getSourceNode().getId(), e.getTargetNode().getId(),
                            e.getConditionExpression()))
                    .toList()
                : Collections.emptyList();

        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getCreatedAt(),
                nodes,
                edges
        );
    }
}
