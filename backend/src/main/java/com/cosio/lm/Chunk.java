package com.cosio.lm;

import java.util.UUID;

import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="chunk")
public class Chunk {
    
    @Id
    @UuidGenerator
    private UUID chunkID;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // @JdbcTypeCode(SqlTypes.VECTOR)
    // @Column(columnDefinition = "vector(384)", nullable = false)
    // private PGvector embedding;
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 384)
    @Column(nullable = false)
    private float[] embedding;
    

    @Column(nullable = false)
    private UUID documentID;

    @ManyToOne(optional = false)
    @JoinColumn(name = "guestID")
    private Guest guest;

    protected Chunk() {}

    public Chunk(String content, float[] embedding, UUID documentID, Guest guest) {
        this.content = content;
        this.embedding = embedding;
        this.documentID = documentID;
        this.guest = guest;
    }

    public UUID getID() {return this.chunkID;}

    public String getContent() {return this.content;}

    public float[] getEmbedding() {return this.embedding;}

    public UUID getDocID() {return this.documentID;}

    public Guest getGuest() {return this.guest;}
}
