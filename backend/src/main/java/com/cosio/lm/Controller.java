package com.cosio.lm;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/")
public class Controller {
    
    /** used to handle mesasges / conversations and handle calls to LLM endpoints*/
    private final ChatService chatService;
    /** used to perform RAG pipeline-relevant services */
    private final EmbeddingService embeddingService;

    // Constructor auto-injected by Spring
    public Controller(ChatService chat, EmbeddingService embeddingService) {
        this.chatService = chat;
        this.embeddingService = embeddingService;
    }

    /**
     * Finds and pushes chat history to the frontend if there is a valid cookie.
     * Else does nothing. This method does not perform any chat genereation.
     * 
     * The response of this endpoint can trigger a change. If valid cookie + chat,
     * history is found, client-side immediately transitions to page dispalying chat.
     * Otherwise frontend flow proceeds as currently is.
     * 
     * @param guestID is a cookie that identfies the user
     * @param response allows for cookies to be sent
     * @return
     */
    @GetMapping("/load")
    public List<ChatMessage> loadConversation(@CookieValue(value = "guestID", required=false) UUID guestID, HttpServletResponse response) {
            return chatService.getHistory(guestID);
    }

    /**
     * To be called repeatedly from frontend as conversation develops
     * Responsible for calling chat service and responding with LLM generations.
     * 
     * Calls {@link ChatService#generate(String, UUID)}
     * 
     * @param prompt to be sent to the LLM
     * @param guestID is a cookie that identifies the user
     * @param response allows for cookies to be sent
     * @return
     */
    @PostMapping("/generate")
    public Flux<String> generate(@RequestParam(value="prompt") String prompt, 
        @CookieValue(value="guestID", required = false) UUID guestID,
        HttpServletResponse response) {

        Guest guest = chatService.resolveGuest(guestID);
        Cookie cookie = new Cookie("guestID", guest.getGuestID().toString());
        cookie.setMaxAge((int)Duration.ofDays(2).toSeconds());
        cookie.setPath("/");
        response.addCookie(cookie);

        Flux<String> result = chatService.generate(prompt, guest);

        // if (guestID == null || !guestID.equals(result.guestID)) {
        // Refresh or set cookie's lifespan for Guest
        // }
        
        return result;
    } 

    /**
     * Endpoint responsible handling a document upload and updating cookie.
     * 
     * @param file to upload as a set of embeddings
     * @param guestID to associate file upload with
     * @param response to return with refreshed cookie
     */
    @PostMapping("/embedDocument")
    public void embedDocument(@RequestParam(value="document") MultipartFile file, 
    @CookieValue(value = "guestID", required = false) UUID guestID, 
    HttpServletResponse response) {

        Guest guest = chatService.resolveGuest(guestID);
        Cookie cookie = new Cookie("guestID", guest.getGuestID().toString());
        cookie.setMaxAge((int)Duration.ofDays(2).toSeconds());
        cookie.setPath("/");
        response.addCookie(cookie);

        embeddingService.embedDocument(file, guest);
    
        return;
    }

}
