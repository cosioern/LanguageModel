package com.cosio.lm;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController("/llm")
public class Service {
    
    @GetMapping("/generate")
    public String generate() {

        WebClient client = WebClient.create("http://localhost:8000");
        String response = client.post()
            .uri("/generate")
            .bodyValue(Map.of("prompt", "Where are you?"))
            .retrieve()
            .bodyToMono(String.class)
            .block();

            return response;
    }

}
