package com.pdk.agents.controller;

import com.pdk.agents.dto.NodeRequest;
import com.pdk.agents.dto.NodeResponse;
import com.pdk.agents.entity.enums.AgentType;
import com.pdk.agents.service.NodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller quản lý Node (Agent).
 */
@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    @PostMapping
    public ResponseEntity<NodeResponse> create(@RequestBody NodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nodeService.create(request));
    }

    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<List<NodeResponse>> findByWorkflow(@PathVariable String workflowId) {
        return ResponseEntity.ok(nodeService.findByWorkflowId(workflowId));
    }

    @GetMapping("/workflow/{workflowId}/type/{agentType}")
    public ResponseEntity<List<NodeResponse>> findByWorkflowAndType(
            @PathVariable String workflowId,
            @PathVariable AgentType agentType) {
        return ResponseEntity.ok(nodeService.findByWorkflowIdAndType(workflowId, agentType));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NodeResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(nodeService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NodeResponse> update(@PathVariable String id, @RequestBody NodeRequest request) {
        return ResponseEntity.ok(nodeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        nodeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
