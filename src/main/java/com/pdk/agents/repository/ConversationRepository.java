package com.pdk.agents.repository;

import com.pdk.agents.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    List<Conversation> findByWorkflowIdOrderByUpdatedAtDesc(String workflowId);
}
