package com.cosio.lm;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    
    private final MessageRepository repo;
    public MessageService(MessageRepository repo) {
        this.repo = repo;
    }

    public Messages saveMessage(Conversations convo, Role role, String content) {
        Messages msg = new Messages(convo, role, content);
        return repo.save(msg);
    }

    public List<Messages> findMessages(Conversations convo) {
        List<Messages> msg = repo.findByConversation(convo);
        return msg;
    }

}
