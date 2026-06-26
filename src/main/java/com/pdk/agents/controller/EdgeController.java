package com.pdk.agents.controller;

import com.pdk.agents.dto.EdgeRequest;
import com.pdk.agents.dto.EdgeResponse;
import com.pdk.agents.service.EdgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller quản lý Edge (kết nối giữa các Agent).
 */
@RestController
@RequestMapping("/api/edges")
@RequiredArgsConstructor
public class EdgeController {

    private final EdgeService edgeService;

    @PostMapping
    public ResponseEntity<EdgeResponse> create(@RequestBody EdgeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(edgeService.create(request));
    }

    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<List<EdgeResponse>> findByWorkflow(@PathVariable String workflowId) {
        return ResponseEntity.ok(edgeService.findByWorkflowId(workflowId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EdgeResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(edgeService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        edgeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
