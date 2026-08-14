package com.cosio.lm;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;

@Entity
public class User extends Account{
    
    @Column(nullable = true, updatable = true, unique = true)
    private String username;

    @Email
    @Column(nullable = true, updatable = true, unique = true)
    private String email;

    @Column(nullable = true, updatable = true)
    private String hashedPassword; 
    
    @Column(nullable = true, updatable = true)
    private UUID verificationToken;

    @Column(nullable = true)
    private boolean verified;

    @Column(nullable = true, updatable = true)
    private String name;

    @Column(nullable = true, updatable = false)
    private LocalDate birthDay;

    @Column(nullable = true, updatable = true)
    private UUID resetToken;

    @Column(nullable = true, updatable = true)
    private Instant tokenExpiry;

    protected User() {}

    public User(String username, String email, String hash, String name, LocalDate birthDate) {
        this.username = username;
        this.email = email;
        this.hashedPassword = hash;
        this.name = name;
        this.birthDay = birthDate;

        verificationToken = UUID.randomUUID();
        verified = false;
    }

    public String getUsername() {return username;}

    public String getEmail() {return email;}

    public String getPassword() {return hashedPassword;}

    public String getName() {return name;}

    public String getBirthday() {return birthDay.toString();}

    public boolean compareToken(UUID token) {return verificationToken.equals(token);}

    public void setVerified() {verified = true;}

    public boolean isVerified() {return verified;}

    public UUID getVerificationToken() {return verificationToken;}

    public void setPassword(String newHash) {hashedPassword = newHash;}

    public boolean isExpired() {return tokenExpiry.isBefore(Instant.now());}

    public void setResetToken() {
        resetToken = UUID.randomUUID();
        tokenExpiry = Instant.now().plusSeconds(3600);
    }

    public UUID getResetToken() {return resetToken;}

    public void invalidateToken() {
        resetToken = null;
        tokenExpiry = null;
    }

    public void invalidateVerificationToken() {
        verificationToken = null;
    }
}
