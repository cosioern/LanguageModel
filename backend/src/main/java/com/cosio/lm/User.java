package com.cosio.lm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.Email;

@Entity
// @Table(name="app_user")
public class User extends Account{
    
    @Column(nullable = false, updatable = true)
    private String username;

    @Email
    @Column(nullable = false, updatable = true, unique = true)
    private String email;

    @Column(nullable = false, updatable = true)
    private String hashedPassword; 
    
    protected User() {}

    public User(String username, String email, String hash) {
        this.username = username;
        this.email = email;
        this.hashedPassword = hash;
    }

    public String getUsername() {return username;}

    public String getEmail() {return email;}

    public String getPassword() {return hashedPassword;}

    // public void setUsername(String newUserName) { this.username = newUserName; }

    // public void setEmail(String newEmail) { this.email = newEmail;}

    // public void setPassword(String newPassword) { this.password = newPassword; }

}
