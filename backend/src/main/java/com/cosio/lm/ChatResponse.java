package com.cosio.lm;
import java.util.UUID;

/**
 * ChatResponse defines data structure holding an LLM repsonse,
 * and the Guest who requested to prompt the LLM;
 */
public class ChatResponse {
        public String response;
        public UUID guestID;
    }
