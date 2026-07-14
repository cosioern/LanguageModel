package com.cosio.lm;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.pgvector.PGvector;


@Entity
@Table(name="chunk")
public class Chunk {
    
    @Id
    @UuidGenerator
    private UUID chunkID;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "vector(384)", nullable = false)
    private PGvector embedding;

    @Column(nullable = false)
    private UUID documentID;

    protected Chunk() {}

    public Chunk(String content, PGvector embedding, UUID documentID) {
        this.content = content;
        this.embedding = embedding;
        this.documentID = documentID;
    }

    public UUID getID() {return this.chunkID;}

    public String getContent() {return this.content;}

    public PGvector getEmbedding() {return this.embedding;}

    public UUID getDocID() {return this.documentID;}

}
