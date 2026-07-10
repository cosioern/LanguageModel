package com.cosio.lm;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Guest
 * Represents a guest to the chatbot site
 * 
 * {guestID, createdAt, lastSeen}
 */
@Entity
@Table(name="guest")
public class Guest {
    
    @Id
    @UuidGenerator
    private UUID guestID;

    @Column(nullable= false, updatable=false)
    private Instant createdAt;

    @Column(nullable = false, updatable = true)
    private Instant lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        lastUpdatedAt = Instant.now();
    }

    /**
     * No argument constructor for JPA
     */
    protected Guest() {}

    public UUID getGuestID() {
        return guestID;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdatedA() {
        return lastUpdatedAt;
    }

    public void updateLastSeen() {
        lastUpdatedAt = Instant.now();
    }
}
