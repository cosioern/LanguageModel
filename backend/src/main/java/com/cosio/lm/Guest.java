package com.cosio.lm;
import jakarta.persistence.Entity;

/**
 * Guest
 * Represents a guest to the chatbot site
 * 
 * {guestID, createdAt, lastSeen}
 */
@Entity
public class Guest extends Account{
    
    /**
     * No argument constructor for JPA
     */
    protected Guest() {}

}
