package com.cosio.lm;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Messages
 * Represents a message from an assistant or user.
 */
@Entity
@Table(name="messages")
public class Messages {
 
    @Id
    @UuidGenerator
    private UUID messageID;

    // @Column(nullable = false)
    // private UUID conversationID;
    @ManyToOne(optional = false)
    @JoinColumn(name = "conversationID")
    private Conversations conversations;

    @Column(nullable = false)
    private int sequenceNum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * No argument constructor for JPA
     */
    protected Messages() {}

    public Messages(Conversations conversations, Role role, String content) {
        this.conversations = conversations;
        this.role = role;
        this.content = content;
    }

    public UUID getMessageID() {
        return messageID;
    }

    public Conversations getConversation() {
        return conversations;
    }

    public int getSequenceNum() {
        return sequenceNum;
    }

    public String getRole() {
        return role.toString().toLowerCase();
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}
