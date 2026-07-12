package com.cosio.lm;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/")
public class Controller {
    
    private final ChatService chat;
    public Controller(ChatService chat) {this.chat = chat;}

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
    public List<ChatMessage> loadConversation(@CookieValue(required=false) UUID guestID, HttpServletResponse response) {
            return chat.getHistory(guestID);
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
    @GetMapping("/generate")
    public String generate(@RequestParam() String prompt, 
        @CookieValue(required = false) UUID guestID,
        HttpServletResponse response) {

        ChatResponse result = chat.generate(prompt, guestID);

        if (guestID == null || !guestID.equals(result.guestID)) {
            Cookie cookie = new Cookie("guestID", result.guestID.toString());
            System.out.println(result.guestID);
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        return result.response;
    } 
}
