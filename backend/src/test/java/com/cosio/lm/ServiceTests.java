package com.cosio.lm;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import com.cosio.lm.AccountService.EmailTakenException;
import com.cosio.lm.AccountService.UsernameTakenException;
import com.cosio.lm.AccountService.VerificationLinkException;
import com.cosio.lm.EmbeddingService.EmbeddedChunk;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    private ConversationRepository convoRepo;
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
    @Mock
    private EmbeddingService esMock;
    @InjectMocks
    private ChatService cs;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(as, "jwtSecret", Base64.getEncoder().encodeToString("your-test-secret-key-at-least-32-bytes-long".getBytes()));
        as.buildKey();
        cs = new ChatService(convoRepo, guestRepo, messageRepo, client, esMock, chunkRepo, accountRepo, userRepo);
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
        Mockito.when(headerSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(float[].class)).thenReturn(Mono.just(new float[]{0.1f, 0.2f, 0.3f}));
        assertNotNull(es.embedPrompt("prompt"));
    }

    @Test
    public void testEmbedDocument() {

        WebClient.RequestBodyUriSpec uriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = Mockito.mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headerSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        
        Mockito.when(client.post()).thenReturn(uriSpec);
        Mockito.when(uriSpec.uri(Mockito.anyString())).thenReturn(bodySpec);
        Mockito.when(bodySpec.contentType(MediaType.MULTIPART_FORM_DATA)).thenReturn(bodySpec);
        Mockito.when(bodySpec.bodyValue(Mockito.any())).thenReturn(headerSpec);
        Mockito.when(headerSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToMono(Mockito.any(ParameterizedTypeReference.class))).thenReturn(Mono.just(List.of(new EmbeddedChunk("some content", new float[]{0.1f, 0.2f}))));

        MultipartFile file = Mockito.mock(MultipartFile.class);
        Resource resource = new ByteArrayResource("some data".getBytes());
        Mockito.when(file.getResource()).thenReturn(resource);
        Mockito.when(file.getOriginalFilename()).thenReturn("filename.txt");

        Account account = Mockito.mock(Account.class);
        es.embedDocument(file, account);

        Mockito.verify(docRepo).save(Mockito.any(Document.class));
        Mockito.verify(chunkRepo).saveAll(Mockito.anyList());

    }

    @Test
    public void testSimilaritySearch() {
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getID()).thenReturn(UUID.randomUUID());
        float[] promptVector = new float[] {0.1f, 0.2f, 0.3f, 0.4f};

        Chunk c1 = Mockito.mock(Chunk.class);
        Chunk c2 = Mockito.mock(Chunk.class);
        Mockito.when(c1.getContent()).thenReturn("content");
        Mockito.when(c2.getContent()).thenReturn("more content");

        Mockito.when(chunkRepo.findSimilarChunks(account.getID(), promptVector, 3)).thenReturn(List.of(c1, c2));
        List<String> results = es.similaritySearch(promptVector, account);

        assertEquals(2, results.size());
        assertTrue(results.contains("content"));
        assertTrue(results.contains("more content"));
    }

    @Test
    public void testGetDocumentNames() {
        Account account = Mockito.mock(Account.class);
        Document doc = Mockito.mock(Document.class);
        Mockito.when(doc.getID()).thenReturn(UUID.randomUUID());
        Mockito.when(doc.getFilename()).thenReturn("Filename");

        Mockito.when(docRepo.findByAccount(account)).thenReturn(List.of());
        assertTrue(es.getDocumentNames(account).isEmpty());
        Mockito.when(docRepo.findByAccount(account)).thenReturn(List.of(doc));
        assertNotNull(es.getDocumentNames(account));
    }

    @Test
    public void testDeleteDocument() {
        UUID documentID = UUID.randomUUID();
        UUID accountID = UUID.randomUUID();
        Document doc = Mockito.mock(Document.class);
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getID()).thenReturn(accountID);

        Mockito.when(docRepo.findById(documentID)).thenReturn(Optional.empty());
        assertFalse(es.deleteDocument(account, documentID));

        Mockito.when(docRepo.findById(documentID)).thenReturn(Optional.of(doc));

        Account differentAccount = Mockito.mock(Account.class);
        Mockito.when(differentAccount.getID()).thenReturn(UUID.randomUUID());
        Mockito.when(doc.getAccount()).thenReturn(differentAccount);
        assertFalse(es.deleteDocument(account, documentID));

        Mockito.when(doc.getAccount().getID()).thenReturn(accountID);
        assertTrue(es.deleteDocument(account, documentID));
    }

    
    // ChatService tests
    @Test
    public void testGenerate() {
        String prompt = "prompt";
        Account account = Mockito.mock(Account.class);
        Conversations convo = Mockito.mock(Conversations.class);
        float[] promptVector = new float[] {1.0f}; // Mockito.mock(float[].class);

        Mockito.when(convoRepo.findByAccount(account)).thenReturn(Optional.empty());
        Mockito.when(convoRepo.save(Mockito.any(Conversations.class))).thenReturn(convo);
        // Mockito.when(messageRepo.findTop5ByConversationsOrderByCreatedAtDesc(convo)).thenReturn(List.of());
        Mockito.when(messageRepo.findTop5ByConversationsOrderByCreatedAtDesc(Mockito.any(Conversations.class))).thenReturn(List.of());
        Mockito.when(esMock.embedPrompt(prompt)).thenReturn(promptVector);
        Mockito.when(esMock.similaritySearch(promptVector, account)).thenReturn(List.of());

        WebClient.RequestBodyUriSpec uriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = Mockito.mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = Mockito.mock(WebClient.ResponseSpec.class);
        
        Mockito.when(client.post()).thenReturn(uriSpec);
        Mockito.when(uriSpec.uri("/generate")).thenReturn(bodySpec);
        Mockito.when(bodySpec.bodyValue(Mockito.any())).thenReturn(headersSpec);
        Mockito.when(headersSpec.retrieve()).thenReturn(responseSpec);
        Mockito.when(responseSpec.bodyToFlux(String.class)).thenReturn(Flux.just("string"));

        cs.generate(prompt, account).collectList().block();
        Mockito.verify(messageRepo, Mockito.times(2)).save(Mockito.any(Messages.class));
    }

    @Test
    public void testGetHistory() {
        assertTrue(cs.getHistory(null).isEmpty());
        Account account = Mockito.mock(Account.class);
        Mockito.when(convoRepo.findByAccount(account)).thenReturn(Optional.empty());
        assertTrue(cs.getHistory(account).isEmpty());
        Mockito.verify(accountRepo).save(account);

        Conversations conversation = Mockito.mock(Conversations.class);
        Mockito.when(convoRepo.findByAccount(account)).thenReturn(Optional.of(conversation));
        List<Messages> messages = List.of(new Messages(conversation, Role.USER, "content"));
        Mockito.when(messageRepo.findByConversationsOrderByCreatedAtAsc(conversation)).thenReturn(messages);
        assertFalse(cs.getHistory(account).isEmpty());
        Mockito.verify(accountRepo, Mockito.times(2)).save(account);
    }

    @Test
    public void clearStaleGuests() {
        Guest g = Mockito.mock(Guest.class);
        Mockito.when(guestRepo.findByLastUpdatedAtBefore(Mockito.any(Instant.class))).thenReturn(List.of());
        cs.clearStaleGuests();
        Mockito.verify(chunkRepo, Mockito.never()).deleteByAccount(g);

        Conversations c = Mockito.mock(Conversations.class);
        Mockito.when(guestRepo.findByLastUpdatedAtBefore(Mockito.any(Instant.class))).thenReturn(List.of(g));
        Mockito.when(convoRepo.findByAccount(g)).thenReturn(Optional.of(c));

        cs.clearStaleGuests();
        Mockito.verify(chunkRepo, Mockito.times(1)).deleteByAccount(g);
        Mockito.verify(messageRepo, Mockito.times(1)).deleteAllByConversations(c);
        Mockito.verify(convoRepo, Mockito.times(1)).delete(c);
        Mockito.verify(guestRepo, Mockito.times(1)).delete(g);
    }

    @Test
    public void testClearUnvalidatedUsers() {

        User user = Mockito.mock(User.class);
        Mockito.when(userRepo.findByVerified(false)).thenReturn(List.of());
        cs.clearUnvalidatedUsers();
        Mockito.verify(userRepo, Mockito.never()).delete(user);

        Mockito.when(userRepo.findByVerified(false)).thenReturn(List.of(user));
        Instant instant = Mockito.mock(Instant.class);
        Mockito.when(user.getCreatedAt()).thenReturn(instant);
        Mockito.when(user.getCreatedAt().isBefore(Mockito.any(Instant.class))).thenReturn(true);
        cs.clearUnvalidatedUsers();
        Mockito.verify(userRepo, Mockito.times(1)).delete(user);
    }

    @Test
    public void testFindGuest() {
        UUID guestID = UUID.randomUUID();
        Guest guest = Mockito.mock(Guest.class);
        Mockito.when(guestRepo.findById(guestID)).thenReturn(Optional.empty());
        assertNull(cs.findGuest(guestID));

        Mockito.when(guestRepo.findById(guestID)).thenReturn(Optional.of(guest));
        assertNotNull(cs.findGuest(guestID));
    }

    @Test
    public void testResolveGuest() {
        UUID guestID = UUID.randomUUID();
        Guest guest = Mockito.mock(Guest.class);

        assertNotEquals(guest, cs.resolveGuest(null));
        Mockito.verify(guestRepo, Mockito.times(1)).save(Mockito.any(Guest.class));
        
        Mockito.when(guestRepo.findById(guestID)).thenReturn(Optional.of(guest));
        assertEquals(guest, cs.resolveGuest(guestID));
        Mockito.verify(guestRepo, Mockito.times(1)).save(Mockito.any(Guest.class));

        Mockito.when(guestRepo.findById(guestID)).thenReturn(Optional.empty());
        assertNotEquals(guest, cs.resolveGuest(guestID));
        Mockito.verify(guestRepo, Mockito.times(2)).save(Mockito.any(Guest.class));
    }
}
