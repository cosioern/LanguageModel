package com.cosio.lm;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversations, UUID>{
    Optional<Conversations> findByAccount(Account account);
    void deleteByAccount(Account account);
}
