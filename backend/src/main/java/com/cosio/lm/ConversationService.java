package com.cosio.lm;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ConversationService {
    

    private final ConversationRepository repo;
    public ConversationService(ConversationRepository repo) {
        this.repo = repo;
    }

    public Conversations createConversation(Guest g) {
        Conversations convo = new Conversations();
        convo.setGuest(g);
        repo.save(convo);
        return convo;
    }

    public Conversations findConversation(Guest g) {
        Optional<Conversations> convo = repo.findByGuest(g);
        if (convo.isPresent())
            return convo.get();
        
        return null;
    }
    
}
