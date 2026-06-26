package com.pdk.agents.repository;

import com.pdk.agents.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    /**
     * Lấy tất cả tin nhắn trong conversation, sắp xếp theo thời gian.
     */
    List<ConversationMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    /**
     * Lấy N tin nhắn gần nhất trong conversation (dùng cho context window).
     */
    List<ConversationMessage> findTop20ByConversationIdOrderByCreatedAtDesc(String conversationId);
}
