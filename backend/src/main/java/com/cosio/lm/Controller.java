package com.cosio.lm;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cosio.lm.AccountService.EmailTakenException;
import com.cosio.lm.AccountService.UsernameTakenException;
import com.cosio.lm.AccountService.VerificationLinkException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Flux;

/**
 * Controller
 * Consists of endpoints handling all traffic to backend
 */
@RestController
@RequestMapping("/")
public class Controller {
    
    /** used to handle mesasges / conversations and handle calls to LM endpoints*/
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
            response.addCookie(generateCookie(token));
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
     * Responsible for calling chat service and responding with LM generations.
     * 
     * Calls {@link ChatService#generate(String, UUID)}
     * 
     * @param prompt    to be sent to the LM
     * @param guestID   is a cookie that identifies the user
     * @param token     JWT token to act as user/session identifier
     * @param response  allows for cookies to be sent
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
           response.addCookie(generateCookie(token));

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
            response.addCookie(generateCookie(token));
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
    public String register(@RequestParam(value="username") String username, 
    @RequestParam(value="email") String email, 
    @RequestParam(value="password") String password,
    @RequestParam(value="name") String name,
    @RequestParam(value="birthDate") LocalDate birthDate,
    HttpServletResponse response) {

        try {
            accountService.createUser(username, email, password, birthDate, name);
        } catch (VerificationLinkException | EmailTakenException | UsernameTakenException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return e.getMessage();
        }
        return "";
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

        Cookie c = generateCookie(token);

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

        response.addCookie(generateCookie(token));
    }
    
    /**
     * Endpoint for handing over account details.
     * Details are for display on frontend Profile page.
     * 
     * @param token     is the session identifier JWT token
     * @param response  contains the cookie
     * @return          mapping of account details
     */
    @GetMapping("/profile")
    public Map<String, String> profile(@CookieValue(value="token", required=true) String token, HttpServletResponse response) {
        User user = accountService.validateToken(token);
        if (user == null) return null;

        Map<String, String> details = accountService.accountDetails(user.getID());
        if (details == null) {
            response.setStatus(401);
            return null;
        }
                
        response.addCookie(generateCookie(token));

        return details;
    }

    private Cookie generateCookie(String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setPath("/");
        cookie.setMaxAge((int)Duration.ofDays(2).toSeconds());
        cookie.setHttpOnly(true);
        return cookie;
    }

    /**
     * Endpoint to determine if a client is a User or a Guest.
     * Used to determine where certain buttons, like Profile or SignUp, redirect.
     * 
     * @param token     JWT token to act as user session identifier
     * @return          true if client is a user, false otherwise
     */
    @GetMapping("/authStatus")
    public boolean authStatus(@CookieValue(value="token", required = false) String token) {

        if (token != null && accountService.validateToken(token) != null) {
            // response.addCookie(generateCookie(token));
            return true;
        }

        return false;
    }

    /**
     * Send the password reset link
     * 
     * @param email     to send the link to
     * @param response  carries status code
     */
    @PostMapping("/forgotPassword")
    public void forgotPassword(@RequestParam(value="email") String email, HttpServletResponse response) {
        if (!accountService.sendPasswordResetLink(email)) {
            response.setStatus(401);
            return;
        }
    }

    /**
     * Handle changing a password and returning a user session identifier
     * 
     * @param resetToken    validates that the user is coming from a server-generated link
     * @param password      is the new password to persist
     * @param response      carries the session cookie
     */
    @PostMapping("/resetPassword")
    public void resetPassword(@RequestParam(value="token") UUID resetToken,
        @RequestParam(value="password") String newPassword, 
        HttpServletResponse response) {

        String token = accountService.changePassword(newPassword, resetToken);
        if (token == null) {response.setStatus(401); return;}

        response.addCookie(generateCookie(token));
    }

    @PostMapping("/logout")
    public void logout(@CookieValue(value = "token", required = true) String token, HttpServletResponse response) {
        User user = accountService.validateToken(token);
        if (user == null) {
            response.setStatus(401);
            return;
        }

        Cookie cookie = new Cookie("token", token);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        return;
    }

}