package com.cosio.lm;
import com.cosio.lm.Controller.Generation;
import com.cosio.lm.Controller.ChatResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


/**
 * ChatService
 * Service interface defining repository operations for managing application users.
 * 
 * This service handles all messages, conversations, guests, and eventually user updates. 
 * This service also handles calls to the microservice generating assistant responses.
 * 
 * @author Ernesto
 */
@Service
public class ChatService {


    private final ConversationRepository convoRepo;
    private final GuestRepository guestRepo;
    private final MessageRepository msgRepo;
    private final String system = "You are PIE, a real estate market analyst."
        + "Provide concise, investment-focused commentary." 
        + "Base reasoning on supply, demand, interest rates, demographics, and valuation."
        + "Avoid speculation and avoid making up specific local statistics."
        + "Prioritize causal explanations and investment implications.";

    // auto-injection by Spring
    public ChatService(ConversationRepository convoRepo, GuestRepository guestRepo, MessageRepository msgRepo) {
        this.convoRepo = convoRepo;
        this.guestRepo = guestRepo;
        this.msgRepo = msgRepo;
    }


    public ChatResponse generate(String prompt, UUID guestID) {

        Guest guest;

        if (guestID == null) {
            guest = createGuest();
        } else {
            guest = findGuest(guestID);
        }

        // if (guest == null) {
        //     guest = createGuest();
        // }

        Conversations convo = findConversation(guest);
        if (convo == null) {
            convo = createConversation(guest);
        }

        // Top 5 desc gets latest responses, reversing orders them chronologically forward
        List<Messages> messages = msgRepo.findTop5ByConversationsOrderBySequenceNumDesc(convo);
        messages = messages.reversed();

        // build
        List<Map<String, String>> history = new ArrayList<>();
        // history.add(Map.of(Role.SYSTEM.toString().toLowerCase(), system));
        // for (Messages m : messages) {history.add(Map.of(m.getRole().toLowerCase(), m.getContent()));}
        // history.add(Map.of(Role.USER.toString().toLowerCase(), prompt));
        history.add(Map.of("role", "system", "content", system));
        for (Messages m : messages) {history.add(Map.of("role", m.getRole(), "content", m.getContent()));}
        history.add(Map.of("role", "user", "content", prompt));


        // history.add(Map.of("prompt", prompt));
        String response = callLLM(history);
        saveMessage(convo, Role.USER, prompt);
        saveMessage(convo, Role.ASSISTANT, response);

        ChatResponse result = new ChatResponse();
        result.guestID = guest.getGuestID();
        result.response = response;
        return result;
    }


    private String callLLM(List<Map<String, String>> history) {

        WebClient client = WebClient.create("http://localhost:8000");
        Generation response = client.post()
            .uri("/generate")
            .bodyValue(Map.of("messages", history))
            .retrieve()
            .bodyToMono(Generation.class)
            .block();

        return response.generation;

    }

    // Conversation services
    private Conversations createConversation(Guest g) {
        Conversations convo = new Conversations();
        convo.setGuest(g);
        convoRepo.save(convo);
        return convo;
    }

    private Conversations findConversation(Guest g) {
        Optional<Conversations> convo = convoRepo.findByGuest(g);
        if (convo.isPresent())
            return convo.get();
        
        return null;
    }

    // Guest services
     private Guest createGuest() {
        Guest g = new Guest();
        guestRepo.save(g);
        return g;
    }

    private Guest findGuest(UUID guestID) {
        Optional<Guest> g = guestRepo.findById(guestID);
        if (g.isPresent()) {
            return g.get();
        }
        return null;
    }

    private void updateLastSeen(Guest g) {
        g.updateLastSeen();
        guestRepo.save(g);
        return;
    }

    // Message services
    private Messages saveMessage(Conversations convo, Role role, String content) {
        Messages msg = new Messages(convo, role, content);
        return msgRepo.save(msg);
    }

    private List<Messages> findMessages(Conversations convo) {
        List<Messages> msg = msgRepo.findByConversations(convo);
        return msg;
    }

}