package com.pdk.agents.service;

import com.pdk.agents.entity.Conversation;
import com.pdk.agents.entity.ConversationMessage;
import com.pdk.agents.entity.Workflow;
import com.pdk.agents.entity.enums.MessageRole;
import com.pdk.agents.exception.ResourceNotFoundException;
import com.pdk.agents.repository.ConversationMessageRepository;
import com.pdk.agents.repository.ConversationRepository;
import com.pdk.agents.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service quản lý phiên hội thoại (Conversation).
 * Hỗ trợ tạo phiên mới, lưu tin nhắn, và lấy context history.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final WorkflowRepository workflowRepository;

    /**
     * Tạo phiên hội thoại mới gắn với Workflow.
     */
    public Conversation createConversation(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID().toString())
                .workflow(workflow)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return conversationRepository.save(conversation);
    }

    /**
     * Lấy hoặc tạo conversation.
     * Nếu conversationId là null → tạo mới.
     */
    public Conversation getOrCreate(String conversationId, String workflowId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        }
        return createConversation(workflowId);
    }

    /**
     * Lưu tin nhắn vào conversation.
     */
    public ConversationMessage addMessage(Conversation conversation, MessageRole role,
                                           String content, String nodeId) {
        ConversationMessage message = ConversationMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .nodeId(nodeId)
                .createdAt(LocalDateTime.now())
                .build();

        // Cập nhật thời gian conversation
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        return messageRepository.save(message);
    }

    /**
     * Lấy lịch sử hội thoại gần nhất (context window cho AI).
     * Trả về dạng chuỗi để inject vào prompt.
     */
    @Transactional(readOnly = true)
    public String getRecentHistory(String conversationId, int maxMessages) {
        List<ConversationMessage> recentMessages =
                messageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId);

        if (recentMessages.isEmpty()) {
            return "";
        }

        // Đảo ngược để có thứ tự chronological
        List<ConversationMessage> ordered = recentMessages.reversed();

        // Giới hạn số message
        List<ConversationMessage> limited = ordered.size() > maxMessages
                ? ordered.subList(ordered.size() - maxMessages, ordered.size())
                : ordered;

        StringBuilder history = new StringBuilder();
        history.append("=== CONVERSATION HISTORY ===\n");
        for (ConversationMessage msg : limited) {
            history.append(String.format("[%s]: %s\n", msg.getRole(), msg.getContent()));
        }
        history.append("=== END HISTORY ===\n");

        return history.toString();
    }
}
