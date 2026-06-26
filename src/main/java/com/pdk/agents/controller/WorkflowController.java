package com.pdk.agents.controller;

import com.pdk.agents.dto.WorkflowRequest;
import com.pdk.agents.dto.WorkflowResponse;
import com.pdk.agents.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller quản lý Workflow.
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    public ResponseEntity<WorkflowResponse> create(@RequestBody WorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> findAll() {
        return ResponseEntity.ok(workflowService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> update(@PathVariable String id, @RequestBody WorkflowRequest request) {
        return ResponseEntity.ok(workflowService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        workflowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
