package com.cosio.lm;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
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
     * @param birthDate user's birthday
     * @param name      user's name
     * @return          a UUID to be compared against token in link
     * 
     * @throws UsernameTakenException       if requested username is taken
     * @throws EmailTakenException          if requested email is taken
     * @throws VerificationLinkException    if link fails to send
     */
    public UUID createUser(String username, String email, String password,LocalDate birthDate, String name) 
        throws UsernameTakenException, EmailTakenException, VerificationLinkException {
        
        if (userRepo.findByUsername(username).isPresent()) {throw new UsernameTakenException();}
        if (userRepo.findByEmail(email).isPresent()) {throw new EmailTakenException();}

        String hash = encoder.encode(password);
        User user = new User(username, email, hash, name, birthDate);
        userRepo.save(user);

        // error sending link, undo user creation
        if (!sendVerificationLink(email, user.getVerificationToken())) {
            userRepo.delete(user);
            throw new VerificationLinkException();
        }

        return user.getVerificationToken();
    }

    /**
     * Authenticate a user.
     * User must be verified (via email link) before they are
     * granted access at login.
     * 
     * @param username  used to identify
     * @param passoword compared against hash stored in persistence
     * @return          a JWT session token on success, null on failure
     */
    public String authenticate(String username, String passoword) {

        // find user
        Optional<User> user = userRepo.findByUsername(username);
        if (user.isEmpty()) { return null; }
        // user must be verified via email link before access is granted
        if (!user.get().isVerified()) { return null; }
        // check if given password, after encoding, matches what's held in persistence
        if (!encoder.matches(passoword, user.get().getPassword())) { return null; }
        // build session token based on user UUID 
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
        user.invalidateVerificationToken();
        userRepo.save(user);
        
        String token = Jwts.builder()
            .subject(user.getID().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + Duration.ofDays(2).toMillis()))
            .signWith(secretKey)
            .compact();

        return token;
    }

    /**
     * Returns account details of User.
     * 
     * @param id to find user
     * @return   mapped user datails
     */
    public Map<String, String> accountDetails(UUID id) {
        Optional<User> user = userRepo.findById(id);
        if (user.isEmpty()) {return null;}

        Map<String, String> details = Map.of(
            "username", user.get().getUsername(), 
            "email", user.get().getEmail(), 
            "birthday", user.get().getBirthday(), 
            "name", user.get().getName()
        );

        return details;
    }

    /**
     * Activate a user's password reset token and token expiry.
     * Send to the user's email a one-time use link with validator token
     * to reset their password.
     * 
     * @param email to send password reset link
     * @return      true if successful, false otherwise
     */
    public boolean sendPasswordResetLink(String email) {

        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return false;
        user.setResetToken();
        userRepo.save(user);

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(email);
        mail.setSubject("Password Reset Link");
        mail.setText("Follow the link to reset your password: http://localhost:5173/reset-password?token="+ user.getResetToken());

        try {mailSender.send(mail);}
        catch (MailException e) {e.printStackTrace(); return false;}

        return true;
    }

    /**
     * If token has not expired, hash and persist user's new password.
     * Build session token for user.
     * 
     * @param newPassword   to encode and persist
     * @param resetToken    to identify a user that had recently requested a password change
     * @return              a session cookie built from user's id
     */
    public String changePassword(String newPassword, UUID resetToken) {

        User user = userRepo.findByResetToken(resetToken).orElse(null);
        if (user == null) {return null;}
        if (user.isExpired()) { return null;}

        String hash  = encoder.encode(newPassword);
        user.setPassword(hash);
        user.invalidateToken();
        userRepo.save(user);

        String token = Jwts.builder()
            .subject(user.getID().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + Duration.ofDays(2).toMillis()))
            .signWith(secretKey)
            .compact();
        
        return token;
    }

    protected class UsernameTakenException extends Exception {
        public UsernameTakenException() {
            super("This username is taken");
        }
    }

    protected class EmailTakenException extends Exception {
        public EmailTakenException() {
            super("This email address is taken");
        }
    }

    protected class VerificationLinkException extends Exception {
        public VerificationLinkException() {
            super("Failed to send verification link");
        }
    }
}
