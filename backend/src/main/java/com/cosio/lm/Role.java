package com.cosio.lm;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Role
 * A message is either from a user, the assistant (LLM) or system (background instruction)
 */
public enum Role {
        USER, ASSISTANT, SYSTEM;

        @JsonValue
        public String toJSON() {
                return this.name().toLowerCase();
        }
}