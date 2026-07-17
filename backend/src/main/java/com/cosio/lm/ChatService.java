package com.cosio.lm;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.pgvector.PGvector;


/**
 * ChatService
 * Service interface handling repository operations such as creating Guests/Conversations/Messages
 * retrieving and saving user/assistant messages while also suppporting conversational memory 
 * for the language-model assistant.
 * 
 * This service handles calls to the microservice generating assistant responses.
 * 
 * @author Ernesto
 */
@Service
public class ChatService {

    /** repo holding guests, who own conversations */
    private final GuestRepository guestRepo;
    /** repo holding conversations, which string together messages */
    private final ConversationRepository convoRepo;
    /** repo holding messages between user and assistant */
    private final MessageRepository msgRepo;
    /** client used to call microservice on localhost port 8000 */
    private final WebClient client;
    /** used to perform RAG pipeline-relevant services */
    private final EmbeddingService embeddingService;

    /** system prompt, to be shipped with each request to LLM */
    private final String system = "You are PIE, a real estate market analyst."
        + "Provide concise, investment-focused commentary." 
        + "Base reasoning on supply, demand, interest rates, demographics, and valuation."
        + "Avoid speculation and avoid making up specific local statistics."
        + "Prioritize causal explanations and investment implications.";

    // auto-injection by Spring
    public ChatService(ConversationRepository convoRepo, GuestRepository guestRepo, 
        MessageRepository msgRepo, WebClient client, EmbeddingService embeddingService) {
        this.convoRepo = convoRepo;
        this.guestRepo = guestRepo;
        this.msgRepo = msgRepo;
        this.client = client;
        this.embeddingService = embeddingService;
    }

    /**
     * Pulls the guest / conversation / messages to build a chat history, prompts the language
     * model, and saves the prompt + response to the proper conversation belonging to the guest
     * in persistence.
     * 
     * @param prompt is the user input to use to direct an llm generation
     * @param guestID identifies the user for correct conversation / message handling
     * @return
     */
    public ChatResponse generate(String prompt, UUID guestID) {

        Guest guest = resolveGuest(guestID);

        Conversations convo = findConversation(guest);
        if (convo == null) {convo = createConversation(guest);}

        // Top 5 desc gets latest responses, reversing orders them chronologically forward
        List<Messages> messages = msgRepo.findTop5ByConversationsOrderByCreatedAtDesc(convo);
        messages = messages.reversed();

        // similarity search + add context to LLM prompt
        PGvector promptVector = embeddingService.embedPrompt(prompt);
        List<String> context = embeddingService.similaritySearch(promptVector, guest);
        String augmentedPrompt = context.isEmpty() ? prompt : "Context:\n" + String.join("\n\n", context) + "\n\nQuestion: " + prompt;

        // build chat history
        List<Map<String, String>> history = new ArrayList<>();
        history.add(Map.of("role", "system", "content", system));
        for (Messages m : messages) {history.add(Map.of("role", m.getRole(), "content", m.getContent()));}
        history.add(Map.of("role", "user", "content", augmentedPrompt));

        // history.add(Map.of("prompt", prompt));
        String response = callLLM(history);
        saveMessage(convo, Role.USER, prompt);
        saveMessage(convo, Role.ASSISTANT, response);

        // package and return result
        ChatResponse result = new ChatResponse();
        result.guestID = guest.getGuestID();
        result.response = response;
        return result;
    }

    /**
     * Retrieve the chat history of a conversation of a given guest.
     * The argument passed is guaranteed to have a value by endpoint:
     * {@link Controller#loadConversation(UUID, jakarta.servlet.http.HttpServletResponse)}
     * 
     * @param guestID identifies a guest, guaranteed to be non-null
     * @return a chat history, or nothing if a conversation is not found
    */
    public List<ChatMessage> getHistory(UUID guestID) {

        // read cookie, retrieve proper guest / conversation
        if (guestID == null) return List.of();
        Guest g = findGuest(guestID);
        if (g == null) return List.of();
        g.updateLastSeen();
        
        Conversations convo = findConversation(g);
        if (convo == null) return List.of();

        // retrieve messages
        List<Messages> msgs = msgRepo.findByConversationsOrderByCreatedAtAsc(convo);
        // if (msgs.isEmpty()) return new ArrayList<ChatMessage>();

        // package chat history into DTO and return
        List<ChatMessage> history = new ArrayList<ChatMessage>();
        for (Messages m: msgs) {
            history.add(
                new ChatMessage(Role.valueOf(m.getRole().toUpperCase()), m.getContent())
            );
        }
        return history;
    }

    /**
     * Endpoint between server and microservice running the LM generations.
     * Context length (history) can be increased by changing findTopXConversatinos... above
     * 
     * @param history is comprised of the last 5 messages for conversational memory
     * @return the LLM's String generation
     */
    private String callLLM(List<Map<String, String>> history) {

        Generation response = client.post()
            .uri("/generate")
            .bodyValue(Map.of("messages", history))
            .retrieve()
            .bodyToMono(Generation.class)
            .block();

        return response.generation;

    }

    /**
     * Clears out the Guest / Conversation / Message repositories
     * for any state Guests (hasn't been updated > 48hrs)
     * Scheduled to run every two days.
     */
    @Scheduled(fixedRate = 2, timeUnit = TimeUnit.DAYS)
    public void clearStaleGuests() {

        Optional<Conversations> c;
        List<Messages> messages;

        for (Guest g : guestRepo.findAll()) {
            // delete related conversation/messages to guest
            if (g.isStale()) {
                c = convoRepo.findByGuest(g);
                if (c.isPresent()) {
                    // clear all messages in a conversation
                    messages = msgRepo.findByConversations(c.get());
                    msgRepo.deleteAll(messages);
                    // delete conversation
                    convoRepo.delete(c.get());
                }
                // delete guest
                guestRepo.delete(g);
            }
        }

    }

    // Guest helper services
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

    public Guest resolveGuest(UUID guestID) {
        Guest guest;
        if (guestID == null) {guest = createGuest();} 
        else {guest = findGuest(guestID);}

        if (guest == null) {guest = createGuest();}
        updateLastSeen(guest);
        
        return guest;
    }

    private void updateLastSeen(Guest g) {
        g.updateLastSeen();
        guestRepo.save(g);
        return;
    }

    // Conversation helper services
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

    // Message helper services
    private Messages saveMessage(Conversations convo, Role role, String content) {
        Messages msg = new Messages(convo, role, content);
        return msgRepo.save(msg);
    }

    // private List<Messages> findMessages(Conversations convo) {
    //     return msgRepo.findByConversations(convo);
    // }

}