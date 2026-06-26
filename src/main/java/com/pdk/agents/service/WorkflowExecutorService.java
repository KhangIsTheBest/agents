package com.pdk.agents.service;

import com.pdk.agents.dto.ChatRequest;
import com.pdk.agents.dto.ChatResponse;
import com.pdk.agents.dto.ExecutionStep;
import com.pdk.agents.dto.WebSocketEvent;
import com.pdk.agents.entity.Conversation;
import com.pdk.agents.entity.Edge;
import com.pdk.agents.entity.Node;
import com.pdk.agents.entity.enums.MessageRole;
import com.pdk.agents.exception.ResourceNotFoundException;
import com.pdk.agents.repository.EdgeRepository;
import com.pdk.agents.repository.NodeRepository;
import com.pdk.agents.repository.WorkflowRepository;
import com.pdk.agents.websocket.AgentWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Workflow Executor — Engine duyệt đồ thị (Graph Traversal) với Intelligence Layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowExecutorService {

    private final WorkflowRepository workflowRepository;
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final AgentExecutionService agentExecutionService;
    private final ConversationService conversationService;
    private final ConditionEvaluator conditionEvaluator;
    private final MemoryService memoryService;
    private final AgentWebSocketHandler agentWebSocketHandler;

    /**
     * Chạy một Workflow từ đầu đến cuối.
     */
    @Transactional
    public ChatResponse execute(ChatRequest request) {
        String workflowId = request.workflowId();

        // Validate workflow
        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        // Load nodes và edges
        List<Node> nodes = nodeRepository.findByWorkflowId(workflowId);
        List<Edge> edges = edgeRepository.findByWorkflowId(workflowId);

        if (nodes.isEmpty()) {
            throw new ResourceNotFoundException("Workflow " + workflowId + " không có Node nào");
        }

        // Tạo hoặc load conversation
        Conversation conversation = conversationService.getOrCreate(
                request.conversationId(), workflowId);

        // Lưu tin nhắn user vào conversation
        conversationService.addMessage(conversation, MessageRole.USER, request.message(), null);

        // Lấy conversation history
        String conversationHistory = conversationService.getRecentHistory(conversation.getId(), 10);

        // Tìm Entry Node
        Node entryNode = findEntryNode(nodes, edges);

        log.info("═══════════════════════════════════════════");
        log.info("▶ Starting Workflow [{}] | Conversation [{}]", workflowId, conversation.getId());
        log.info("▶ Nodes: {} | Edges: {} | Entry: [{}] ({})",
                nodes.size(), edges.size(), entryNode.getNodeName(), entryNode.getAgentType());
        log.info("═══════════════════════════════════════════");

        // Duyệt đồ thị
        List<ExecutionStep> executionLog = new ArrayList<>();
        String currentInput = request.message();
        Node currentNode = entryNode;
        int stepOrder = 1;
        String finalOutput = "";

        try {
            while (currentNode != null) {
                final Node activeNode = currentNode;

                // Gửi sự kiện Node bắt đầu chạy qua WebSocket
                agentWebSocketHandler.broadcastEvent(new WebSocketEvent(
                        "NODE_RUNNING",
                        workflowId,
                        conversation.getId(),
                        activeNode.getId(),
                        activeNode.getNodeName(),
                        activeNode.getAgentType().name(),
                        "RUNNING",
                        null,
                        null
                ));

                // Tìm relevant memories cho Agent này (RAG)
                String memoryContext = "";
                try {
                    List<String> memories = memoryService.searchRelevant(activeNode.getId(), currentInput);
                    memoryContext = memoryService.formatAsContext(memories);
                } catch (Exception e) {
                    log.warn("⚠ Memory search failed for Agent [{}]: {}", activeNode.getNodeName(), e.getMessage());
                }

                // Thực thi Agent với đầy đủ context
                String output = agentExecutionService.execute(
                        activeNode, currentInput, conversationHistory, memoryContext);

                // Tạo đối tượng bước thực thi
                ExecutionStep step = new ExecutionStep(
                        stepOrder,
                        activeNode.getId(),
                        activeNode.getNodeName(),
                        activeNode.getAgentType().name(),
                        currentInput,
                        output
                );
                executionLog.add(step);

                // Gửi sự kiện Node hoàn thành chạy kèm kết quả qua WebSocket
                agentWebSocketHandler.broadcastEvent(new WebSocketEvent(
                        "NODE_COMPLETED",
                        workflowId,
                        conversation.getId(),
                        activeNode.getId(),
                        activeNode.getNodeName(),
                        activeNode.getAgentType().name(),
                        "COMPLETED",
                        step,
                        null
                ));

                // Lưu response của Agent vào conversation
                conversationService.addMessage(conversation, MessageRole.ASSISTANT,
                        output, activeNode.getId());

                // Lưu output vào semantic memory (cho RAG tương lai)
                try {
                    memoryService.store(activeNode.getId(), output);
                } catch (Exception e) {
                    log.warn("⚠ Memory store failed for Agent [{}]: {}", activeNode.getNodeName(), e.getMessage());
                }

                finalOutput = output;

                // ===== BRANCHING LOGIC =====
                List<Edge> outgoingEdges = edges.stream()
                        .filter(e -> e.getSourceNode().getId().equals(activeNode.getId()))
                        .toList();

                if (outgoingEdges.isEmpty()) {
                    log.info("■ Workflow completed at Node [{}] after {} steps",
                            activeNode.getNodeName(), stepOrder);
                    break;
                }

                // Đánh giá conditions
                Node nextNode = evaluateAndSelectNextNode(outgoingEdges, output, nodes);

                if (nextNode == null) {
                    log.info("■ No matching edge condition — workflow ends at [{}]",
                            activeNode.getNodeName());
                    break;
                }

                currentNode = nextNode;
                currentInput = output;
                stepOrder++;
            }

            // Gửi sự kiện toàn bộ Workflow hoàn thành qua WebSocket
            agentWebSocketHandler.broadcastEvent(new WebSocketEvent(
                    "WORKFLOW_COMPLETED",
                    workflowId,
                    conversation.getId(),
                    null,
                    null,
                    null,
                    "COMPLETED",
                    null,
                    finalOutput
            ));

        } catch (Exception e) {
            log.error("❌ Lỗi thực thi workflow: ", e);
            // Gửi sự kiện Workflow thất bại qua WebSocket
            agentWebSocketHandler.broadcastEvent(new WebSocketEvent(
                    "WORKFLOW_FAILED",
                    workflowId,
                    conversation.getId(),
                    null,
                    null,
                    null,
                    "FAILED",
                    null,
                    e.getMessage()
            ));
            throw e; // Ném ngược ngoại lệ để rollback / trả về HTTP Error
        }

        return new ChatResponse(workflowId, conversation.getId(), finalOutput, executionLog);
    }

    /**
     * Đánh giá conditions trên các outgoing edges và chọn Node tiếp theo.
     *
     * Ưu tiên:
     * 1. Edge có condition cụ thể (contains, equals...) match trước
     * 2. Edge "default" làm fallback
     * 3. Edge đầu tiên nếu không có condition nào
     */
    private Node evaluateAndSelectNextNode(List<Edge> outgoingEdges, String output, List<Node> allNodes) {
        Edge selectedEdge = null;
        Edge defaultEdge = null;

        for (Edge edge : outgoingEdges) {
            String condition = edge.getConditionExpression();

            // Tìm default edge
            if (conditionEvaluator.isDefaultEdge(condition)) {
                defaultEdge = edge;
                continue;
            }

            // Đánh giá condition
            if (conditionEvaluator.evaluate(condition, output)) {
                selectedEdge = edge;
                log.info("→ Branching: condition [{}] matched → next node [{}]",
                        condition, edge.getTargetNode().getId());
                break;
            }
        }

        // Chọn: matched edge > default edge > edge đầu tiên
        Edge nextEdge = selectedEdge != null ? selectedEdge
                : defaultEdge != null ? defaultEdge
                : outgoingEdges.get(0);

        String nextNodeId = nextEdge.getTargetNode().getId();
        return allNodes.stream()
                .filter(n -> n.getId().equals(nextNodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Tìm Entry Node — node không có incoming edge, ưu tiên ROUTER.
     */
    private Node findEntryNode(List<Node> nodes, List<Edge> edges) {
        Set<String> targetNodeIds = edges.stream()
                .map(e -> e.getTargetNode().getId())
                .collect(Collectors.toSet());

        List<Node> entryNodes = nodes.stream()
                .filter(n -> !targetNodeIds.contains(n.getId()))
                .toList();

        if (entryNodes.isEmpty()) {
            log.warn("⚠ Không tìm thấy Entry Node rõ ràng, sử dụng node đầu tiên");
            return nodes.get(0);
        }

        return entryNodes.stream()
                .filter(n -> n.getAgentType().name().equals("ROUTER"))
                .findFirst()
                .orElse(entryNodes.get(0));
    }
}
