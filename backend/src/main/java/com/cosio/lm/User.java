package com.cosio.lm;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;

@Entity
// @Table(name="app_user")
public class User extends Account{
    
    @Column(nullable = false, updatable = true, unique = true)
    private String username;

    @Email
    @Column(nullable = false, updatable = true, unique = true)
    private String email;

    @Column(nullable = false, updatable = true)
    private String hashedPassword; 
    
    @Column(nullable = false, updatable = false)
    private UUID verificationToken;

    @Column(nullable = false)
    private boolean verified;

    protected User() {}

    public User(String username, String email, String hash) {
        this.username = username;
        this.email = email;
        this.hashedPassword = hash;

        verificationToken = UUID.randomUUID();
        verified = false;
    }

    public String getUsername() {return username;}

    public String getEmail() {return email;}

    public String getPassword() {return hashedPassword;}

    public boolean compareToken(UUID token) {return verificationToken.equals(token);}

    public void setVerified() {verified = true;}

    public boolean isVerified() {return verified;}

    public UUID getVerificationToken() {return verificationToken;}

}
