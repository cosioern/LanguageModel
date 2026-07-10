package com.cosio.lm;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name="Conversations")
public class Conversations {
    
    @Id
    @UuidGenerator
    private UUID conversationID;

    @ManyToOne(optional = false)
    @JoinColumn(name = "guestID")
    private Guest guest;

    // @Column(nullable = false)
    // private String title;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * No argument constructor for JPA
     */
    protected Conversations() {}

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getConversationID() {
        return conversationID;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setGuest(Guest g) {
        this.guest = g;
    }

    // instead, use Guest.lastUpdatedAt to clear guest + their conversation
    // user's conversation(s) won't be cleared
    // @Column(nullable = false, updatable = true)
    // private Instant lastUpdatedAt;
    
}
