package com.cosio.lm;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * AccountService
 * 
 * Handles user creation, verification, and authentication.
 * @author Ernesto
 */
@Service
public class AccountService {
    
    
    private final String jwtSecret;
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder encoder;
    private final JavaMailSender mailSender;
    private SecretKey secretKey;

    public AccountService(UserRepository userRepo, BCryptPasswordEncoder encoder, 
        JavaMailSender mailSender,  @Value("${jwt.secret}")String jwtSecret) {

        this.userRepo = userRepo;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    public void buildKey() {
        this.secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));
    }

    /**
     * Creates a user account and saves to persistence.
     * Walks back changes if verification link fails to send.
     * 
     * @param username  requested by user
     * @param email     requiested to be used to verify account
     * @param password  requested to authenticate access
     * @return          a UUID to be compared against token in link
     */
    public UUID createUser(String username, String email, String password)  {
        
        // replace with custom exceptions?
        if (userRepo.findByUsername(username).isPresent() || userRepo.findByEmail(email).isPresent()) {
            return null;
        }

        String hash = encoder.encode(password);
        User user = new User(username, email, hash);
        userRepo.save(user);

        // error sending link, undo user creation
        if (!sendVerificationLink(email, user.getVerificationToken())) {
            userRepo.delete(user);
            return null;
        }

        return user.getVerificationToken();
    }

    /**
     * Authenticate a user.
     * 
     * @param username  used to identify
     * @param passoword compared against hash stored in persistence
     * @return          a JWT session token on success, null on failure
     */
    public String authenticate(String username, String passoword) {

        // SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret));

        Optional<User> user = userRepo.findByUsername(username);
        if (user.isEmpty())
            return null;

        if (!encoder.matches(passoword, user.get().getPassword())) {
            return null;
        }
        
        String token = Jwts.builder()
            .subject(user.get().getID().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + Duration.ofDays(2).toMillis()))
            .signWith(secretKey)
            .compact();

        return token;
    }

    /**
     * Validate a  user account by comparing argument token
     * with toke held in persistence.
     * Used on every authenticated request.
     * 
     * @param token to compare with User's validationToken
     * @return verified user on success, null on failure
     */
    public User validateToken(String token) {
        if (token == null) return null;

        Claims claim;
        try {
            claim = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException e) {
            return null;
        }
        
        UUID userID = UUID.fromString(claim.getSubject());
        User user = userRepo.findById(userID).orElse(null);
        if (user != null) {
            user.updateLastSeen();
            userRepo.save(user);
        }
        return user;
    }

    /**
     * Send a verification link when creating a user.
     * 
     * @param email             is destination to send link
     * @param verificationToken is sent with link to be used for comparison later
     * @return true on success, false otherwise
     */
    private boolean sendVerificationLink(String email, UUID verificationToken) {

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(email);
        mail.setSubject("Verification Link");
        mail.setText("Click to verify: http://localhost:5173/verify?token=" + verificationToken);
        
        // failing to send message goes up call chain and undoes user
        try {mailSender.send(mail);} 
        catch (MailException e) {e.printStackTrace(); return false;}

       return true;
    }

    /**
     * Verifies a a new user after link is used.
     * 
     * @param verificationToken used to lookup new account
     * @return JWT token on success, null on failure
     */
    public String verifyUser(UUID verificationToken) {
        // compareToken redundant check?
        User user = userRepo.findByVerificationToken(verificationToken).orElse(null);
        if (user==null || !user.compareToken(verificationToken)) {
            return null;
        }
        user.setVerified();
        userRepo.save(user);
        
        String token = Jwts.builder()
            .subject(user.getID().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + Duration.ofDays(2).toMillis()))
            .signWith(secretKey)
            .compact();

        return token;
    }

}
