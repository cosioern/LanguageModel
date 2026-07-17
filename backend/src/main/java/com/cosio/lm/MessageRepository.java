package com.cosio.lm;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Messages, UUID> {
    List<Messages> findByConversations(Conversations convo);
    // List<Messages> findTop5ByConversationsOrderBySequenceNumDesc(Conversations convo);
    // List<Messages> findTop10ByConversationsOrderBySequenceNumDesc(Conversations convo);
    // List<Messages> findTop20ByConversationsOrderBySequenceNumDesc(Conversations convo);
    List<Messages> findByConversationsOrderByCreatedAtAsc(Conversations convo);
    List<Messages> findTop5ByConversationsOrderByCreatedAtDesc(Conversations convo);
    List<Messages> findTop10ByConversationsOrderByCreatedAtDesc(Conversations convo);
    List<Messages> findTop20ByConversationsOrderByCreatedAtDesc(Conversations convo);
    void deleteAllByConversations(Conversations convo);
}