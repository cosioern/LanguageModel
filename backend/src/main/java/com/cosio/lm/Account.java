package com.cosio.lm;

import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name="account")
@Inheritance(strategy = InheritanceType.JOINED)
public class Account {
    
    @Id
    @UuidGenerator
    private UUID accountID;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, updatable = true)
    private Instant lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        lastUpdatedAt = Instant.now();
    } 

    public Account() {}

    public UUID getID() {return accountID;}

    public Instant getCreatedAt() {return createdAt;}

    public void updateLastSeen() {lastUpdatedAt = Instant.now();}

}
