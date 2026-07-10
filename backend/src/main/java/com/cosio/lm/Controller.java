package com.cosio.lm;

import java.util.Map;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@CrossOrigin(origins="http://localhost:5173")
@RequestMapping("/llm")
public class Controller {
    
    /**
     * Endpoint between microservice and react
     * @param prompt
     * @return
     */
    @GetMapping("/generate")
    public String generate(@RequestParam("prompt") String prompt, 
    @CookieValue(value = "guestID", required = false) String guestID) {

        // if (guestID = null)
        //     guestID = guestService.createGuest();

        WebClient client = WebClient.create("http://localhost:8000");
        Generation response = client.post()
            .uri("/generate")
            .bodyValue(Map.of("prompt", prompt))
            .retrieve()
            .bodyToMono(Generation.class)
            .block();

            return response.generation;
    }

    static final class Generation {
        public String generation;
    }

}
