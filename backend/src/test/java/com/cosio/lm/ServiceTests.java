package com.cosio.lm;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.cosio.lm.AccountService.EmailTakenException;
import com.cosio.lm.AccountService.UsernameTakenException;
import com.cosio.lm.AccountService.VerificationLinkException;

import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ServiceTests {
    
    @Mock
    private UserRepository userRepo;
    @Mock
    private AccountRepository accountRepo;
    @Mock 
    private GuestRepository guestRepo;
    @Mock
    private DocumentRepository docRepo;
    @Mock
    private ChunkRepository chunkRepo;
    @Mock
    private MessageRepository messageRepo;
    @Mock
    private BCryptPasswordEncoder encoder;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private WebClient client;
    @InjectMocks
    private AccountService as;
    @InjectMocks
    private EmbeddingService es;
    @InjectMocks
    private ChatService cs;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(as, "jwtSecret", Base64.getEncoder().encodeToString("your-test-secret-key-at-least-32-bytes-long".getBytes()));
        as.buildKey();
    }

    // AccountService tests
    @Test
    public void testCreateUser() throws UsernameTakenException, EmailTakenException, VerificationLinkException {
        UUID verificationToken = as.createUser("Username", "email@email.com", "password", LocalDate.now(), "Name");
        assertNotNull(verificationToken);

        Mockito.when(userRepo.findByUsername("Username")).thenReturn(Optional.of(new User()));
        assertThrows(UsernameTakenException.class, () -> as.createUser("Username", null, null, null, null));
        Mockito.when(userRepo.findByEmail("email@email.com")).thenReturn(Optional.of(new User()));
        assertThrows(EmailTakenException.class, () -> as.createUser("Username2", "email@email.com", null, null, null));
        Mockito.doThrow(new MailSendException("fail")).when(mailSender).send(Mockito.any(SimpleMailMessage.class));
        // test the private method sendVerificatoinLink
        assertThrows(VerificationLinkException.class, () -> as.createUser(null, "", null, null, null));
    }

    @Test
    public void testAuthenticate() {

        // user.isEmpty() check
        Mockito.when(userRepo.findByUsername("")).thenReturn(Optional.empty());
        assertNull(as.authenticate("", ""));

        // user.isVerified() check
        User user = Mockito.mock(User.class);
        Mockito.when(userRepo.findByUsername("")).thenReturn(Optional.of(user));
        assertNull(as.authenticate("", ""));

        // password match check
        user = Mockito.mock(User.class);
        Mockito.when(user.isVerified()).thenReturn(true);
        Mockito.when(user.getPassword()).thenReturn("hash");
        Mockito.when(user.getID()).thenReturn(UUID.randomUUID());
        Mockito.when(userRepo.findByUsername("")).thenReturn(Optional.of(user));
        Mockito.when(encoder.matches("password", "hash")).thenReturn(false);
        assertNull(as.authenticate("", "password"));
        Mockito.when(encoder.matches("password", "hash")).thenReturn(true);
        assertNotNull(as.authenticate("", "password"));        
    }

    @Test
    public void testValidateToken() {
        UUID userID = UUID.randomUUID();
        User user = Mockito.mock(User.class);
        Mockito.when(user.isVerified()).thenReturn(true);
        Mockito.when(user.getPassword()).thenReturn("hash");
        Mockito.when(user.getID()).thenReturn(userID);
        Mockito.when(userRepo.findByUsername("")).thenReturn(Optional.of(user));
        // Mockito.when(encoder.matches("password", "hash")).thenReturn(false);
        Mockito.when(encoder.matches("password", "hash")).thenReturn(true);

        // validate token check
        String token = as.authenticate("", "password");
        assertNotNull(token);
        assertNull(as.validateToken(null));
        Mockito.when(userRepo.findById(userID)).thenReturn(Optional.of(user));
        assertNull(as.validateToken("not-a-cryptographically-signed-jws-claim"));

        assertNull(as.validateToken("bad token"));
        assertNotNull(as.validateToken(token));
        assertTrue(as.validateToken(token) instanceof User);
    }

    @Test
    public void testVerifyUser() {
        UUID userID = UUID.randomUUID();
        User user = Mockito.mock(User.class);

        assertNull(as.verifyUser(null));

        Mockito.when(userRepo.findByVerificationToken(userID)).thenReturn(Optional.of(user));
        Mockito.when(user.compareToken(userID)).thenReturn(false);
        assertNull(as.verifyUser(userID));

        Mockito.when(user.compareToken(userID)).thenReturn(true);
        Mockito.when(user.getID()).thenReturn(userID);
        Mockito.when(userRepo.findByVerificationToken(userID)).thenReturn(Optional.of(user));
        assertNotNull(as.verifyUser(userID));

    }

    @Test
    public void testAccountDetails() {

        assertNull(as.accountDetails(null));

        User user = Mockito.mock(User.class);
        UUID userID = UUID.randomUUID();
        Mockito.when(userRepo.findById(userID)).thenReturn(Optional.of(user));
        Mockito.when(user.getUsername()).thenReturn("user1");
        Mockito.when(user.getEmail()).thenReturn("email@email.com");
        Mockito.when(user.getBirthday()).thenReturn(LocalDate.now().toString());
        Mockito.when(user.getName()).thenReturn("Jane");

        assertNotNull(as.accountDetails(userID));
        assertTrue(as.accountDetails(userID).get("username").equals("user1"));
        assertTrue(as.accountDetails(userID).get("email").equals("email@email.com"));
        assertNotNull(as.accountDetails(userID).get("birthday"));
        assertTrue(as.accountDetails(userID).get("name").equals("Jane"));
    }

    @Test
    public void testSendPasswordResetLink() {

        assertFalse(as.sendPasswordResetLink(null));
        String email = "email@email.com";
        User user = Mockito.mock(User.class);
        Mockito.when(userRepo.findByEmail(email)).thenReturn(Optional.of(user));
    
        Mockito.doThrow(new MailSendException("Test Exception")).when(mailSender).send(Mockito.any(SimpleMailMessage.class));
        assertFalse(as.sendPasswordResetLink(email));

        Mockito.doNothing().when(mailSender).send(Mockito.any(SimpleMailMessage.class));
        assertTrue(as.sendPasswordResetLink(email));
    }

    @Test
    public void testChangePassword() {
        UUID resetToken = UUID.randomUUID();
        User user = Mockito.mock(User.class);
        // Mockito.when(user.getResetToken()).thenReturn(resetToken);

        Mockito.when(userRepo.findByResetToken(resetToken)).thenReturn(Optional.empty());
        // assertNull(userRepo.findByResetToken(null));
        assertNull(as.changePassword(null, resetToken));
        Mockito.when(userRepo.findByResetToken(resetToken)).thenReturn(Optional.of(user));
        Mockito.when(user.isExpired()).thenReturn(true);
        assertNull(as.changePassword(null, resetToken));

        Mockito.when(user.getID()).thenReturn(UUID.randomUUID());
        Mockito.when(user.isExpired()).thenReturn(false);
        assertNotNull(as.changePassword(null, resetToken));

    }

    @Test
    public void testUpdatePassword() {

        User user = Mockito.mock(User.class);
        String currentPassword = "currentPassword";
        String newPassword = "newPassword";
        Mockito.when(user.getPassword()).thenReturn("hash");
        
        Mockito.when(encoder.matches(currentPassword, "hash")).thenReturn(false);
        assertFalse(as.updatePassword(currentPassword, newPassword, user));

        Mockito.when(encoder.matches(currentPassword, "hash")).thenReturn(true);
        assertTrue(as.updatePassword(currentPassword, newPassword, user));
    }


    // EmbeddingService tests
    @Test
    public void testEmbedPrompt() {
        WebClient.RequestBodyUriSpec uriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = Mockito.mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headerSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
    
        Mockito.when(client.post()).thenReturn(uriSpec);
        Mockito.when(uriSpec.uri(Mockito.anyString())).thenReturn(bodySpec);
        Mockito.when(bodySpec.bodyValue(Mockito.any())).thenReturn(headerSpec);
        // Mockito.when(bodySpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(headerSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(float[].class)).thenReturn(Mono.just(new float[]{0.1f, 0.2f, 0.3f}));
        assertNotNull(es.embedPrompt("prompt"));
    }
}
