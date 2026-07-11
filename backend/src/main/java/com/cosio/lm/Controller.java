package com.cosio.lm;

import java.util.UUID;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;


@RestController
@CrossOrigin(origins="http://localhost:5173")
@RequestMapping("/llm")
public class Controller {
    
    private final ChatService chat;
    public Controller(ChatService chat) {this.chat = chat;}


    /**
     * Endpoint between microservice and react
     * @param prompt
     * @return
     */
    @GetMapping("/generate")
    public String generate(@RequestParam("prompt") String prompt, 
    @CookieValue(value = "guestID", required = false) UUID guestID,
    HttpServletResponse response) {

        ChatResponse result = chat.generate(prompt, guestID);

        if (guestID == null) {
            Cookie cookie = new Cookie("guestID", result.guestID.toString());
            cookie.setPath("/");
            response.addCookie(cookie);
        }

        return result.response;
    }

    // for DTO structuring
    static final class Generation {
        public String generation;
    }
    // for ...
    static final class ChatResponse {
        public String response;
        public UUID guestID;
    }

}
