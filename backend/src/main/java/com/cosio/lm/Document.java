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
@Table(name="Document")
public class Document {

    @Id
    @UuidGenerator
    private UUID documentID;

    @Column(nullable=false)
    private String filename;

    @ManyToOne(optional=false)
    @JoinColumn(name = "accountID")
    private Account account;

    @Column(nullable=false, updatable = false)
    private Instant uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = Instant.now();
    }

    protected Document() {}

    /**
     * Used by services to persist Document information and
     * associate wtih Chunks.
     * 
     * @param filename of uploaded document
     * @param account  of user
     */
    public Document(String filename, Account account) {
        this.filename = filename;
        this.account = account;
    }

    public String getFilename() {return filename;}

    public UUID getID() {return documentID;}

    public Account getAccount() {return account;}
}