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
    /** used to perform user-relevant services */
    private final AccountService accountService;

    // Constructor auto-injected by Spring
    public Controller(ChatService chat, EmbeddingService embeddingService, AccountService accountService) {
        this.chatService = chat;
        this.embeddingService = embeddingService;
        this.accountService = accountService;
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
    public List<ChatMessage> loadConversation(@CookieValue(value = "guestID", required=false) UUID guestID, 
    @CookieValue(value = "token", required = false) String token, HttpServletResponse response) {
        
        // load for an authen user
        User user = accountService.validateToken(token);
        if (user != null) { 
            Cookie cookie = new Cookie("token", token);
            cookie.setPath("/");
            cookie.setMaxAge((int)Duration.ofDays(2).toSeconds());
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
            
            return chatService.getHistory(user); 
        }

        // istead, load for a guest
        if (guestID == null) return List.of();
        Guest g = chatService.findGuest(guestID);
        Cookie cookie = new Cookie("guestID", guestID.toString());
        cookie.setMaxAge((int)Duration.ofDays(2).toSeconds());
        cookie.setPath("/");
        response.addCookie(cookie);
        return chatService.getHistory(g);
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
        @CookieValue(value="token", required = false) String token,
        HttpServletResponse response) {

        // generation pathway for an authen user
        User user = accountService.validateToken(token);
        if (user != null) { 
           Cookie cookie = new Cookie("token", token);
           cookie.setMaxAge((int)Duration.ofDays(2).toSeconds());
           cookie.setPath("/");
           cookie.setHttpOnly(true);
           response.addCookie(cookie);

           Flux<String> result = chatService.generate(prompt, user);
           return result;
        }

        // generation pathway for a guest
        Guest guest = chatService.resolveGuest(guestID);
        Cookie cookie = new Cookie("guestID", guest.getID().toString());
        cookie.setMaxAge((int)Duration.ofDays(2).toSeconds());
        cookie.setPath("/");
        response.addCookie(cookie);

        Flux<String> result = chatService.generate(prompt, guest);
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
    @CookieValue(value = "token", required = false) String token,
    HttpServletResponse response) {

        User user = accountService.validateToken(token);
        if (user != null) { 
            Cookie cookie = new Cookie("token", token);
            cookie.setMaxAge((int)Duration.ofDays(2).toSeconds());
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            response.addCookie(cookie);

            embeddingService.embedDocument(file, user);
            return; 
        }

        Guest guest = chatService.resolveGuest(guestID);
        Cookie cookie = new Cookie("guestID", guest.getID().toString());
        cookie.setMaxAge((int)Duration.ofDays(2).toSeconds());
        cookie.setPath("/");
        response.addCookie(cookie);

        embeddingService.embedDocument(file, guest);
    
        return;
    }

    /**
     * User account registration endpoint.
     * 
     * @param username  requested by user
     * @param email     to verify account
     * @param password  to restrict access to account
     * @param response  to convey success/failure
     */
    @PostMapping("/register")
    public void register(@RequestParam(value="username") String username, 
    @RequestParam(value="email") String email, 
    @RequestParam(value="password") String password,
    HttpServletResponse response) {

        UUID token = accountService.createUser(username, email, password);
        // failure to send email => user removed, send failure status code
        if (token == null) {response.setStatus(401);}
    }

    /**
     * User login endpoint.
     * 
     * @param username to identify user
     * @param password to authenticate login
     * @param response to send session cookie
     */
    @PostMapping("/login")
    public void login(@RequestParam(value="username") String username, 
    @RequestParam(value="password") String password, 
    HttpServletResponse response) {

        String token = accountService.authenticate(username, password);

        if (token == null) {
            response.setStatus(401);
            return;
        }

        Cookie c = new Cookie("token", token);
        c.setMaxAge((int)Duration.ofDays(2).toSeconds());
        c.setPath("/");
        c.setHttpOnly(true);

        response.addCookie(c);

    }

    /**
     * New user verification endpoint.
     * 
     * @param verificationToken verifies email by matching with User's value held in persistence
     * @param response to send session cookie
     */
    @GetMapping("/verify")
    public void verify(@RequestParam(value="token") UUID verificationToken, HttpServletResponse response) {

        // apply session token
        String token = accountService.verifyUser(verificationToken);
        if (token == null) {response.setStatus(401); return;}
        Cookie cookie = new Cookie("token", token);
        cookie.setMaxAge((int)Duration.ofDays(2).toSeconds());
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        response.addCookie(cookie);
    }

}
