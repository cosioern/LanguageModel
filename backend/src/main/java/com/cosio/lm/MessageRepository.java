package com.cosio.lm;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Messages, UUID> {
    List<Messages> findByConversation(Conversations convo);
}