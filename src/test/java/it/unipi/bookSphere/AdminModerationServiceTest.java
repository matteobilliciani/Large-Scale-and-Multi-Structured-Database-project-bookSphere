package it.unipi.bookSphere;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.exceptions.*;
import it.unipi.bookSphere.model.mongodb.*;
import it.unipi.bookSphere.model.neo4j.UserNode;
import it.unipi.bookSphere.repository.mongo.*;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.service.AdminModerationService;
import it.unipi.bookSphere.service.ReviewService;
import it.unipi.bookSphere.utils.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminModerationServiceTest {

    @Autowired
    private AdminModerationService adminModerationService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    private static final String ADMIN_ID = "admin_user_id";
    private static final String USER_ID = "banned_user_id";
    private static final String BOOK_ID = "book_for_ban_test";

    // Tracciamento dei dati creati per il cleanup isolato
    private List<String> createdReviewIds = new ArrayList<>();
    private List<String> createdUserIds = new ArrayList<>();
    private List<String> createdUserNodeIds = new ArrayList<>();
    private List<String> createdBookIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // Resetta liste di tracciamento
        createdReviewIds.clear();
        createdUserIds.clear();
        createdUserNodeIds.clear();
        createdBookIds.clear();

        // Create a test user se non esiste
        if (!userRepository.existsById(USER_ID)) {
            RegisteredUser user = new RegisteredUser();
            user.setId(USER_ID);
            user.setUsername("user_to_ban");
            user.setStatus("active");
            user.setReviews(new ArrayList<>());
            userRepository.save(user);
            createdUserIds.add(USER_ID);
        }

        // Create a Neo4j node for the user se non esiste
        if (!userNodeRepository.findByMongoId(USER_ID).isPresent()) {
            UserNode userNode = new UserNode();
            userNode.setMongoId(USER_ID);
            userNode.setUsername("user_to_ban");
            userNodeRepository.save(userNode);
            createdUserNodeIds.add(USER_ID);
        }

        // Create a test book se non esiste
        if (!bookRepository.existsById(BOOK_ID)) {
            BookDocument book = new BookDocument();
            book.setId(BOOK_ID);
            book.setTitle("Test Book");
            book.setAvailability("ACTIVE");
            book.setStatsPerYear(new ArrayList<>());
            book.setReviews(new ArrayList<>());
            BookDocument.Author author = new BookDocument.Author("auth_1", "Auth Name");
            book.setAuthor(author);
            bookRepository.save(book);
            createdBookIds.add(BOOK_ID);
        }

        setupAdminContext();
    }

    @AfterEach
    void cleanup() {
        // Elimina solo i dati creati da questo test
        createdReviewIds.forEach(id -> {
            if (reviewRepository.existsById(id)) {
                reviewRepository.deleteById(id);
            }
        });
        createdBookIds.forEach(id -> {
            if (bookRepository.existsById(id)) {
                bookRepository.deleteById(id);
            }
        });
        createdUserIds.forEach(id -> {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
            }
        });
        createdUserNodeIds.forEach(id -> {
            if (userNodeRepository.findByMongoId(id).isPresent()) {
                userNodeRepository.deleteByMongoId(id);
            }
        });
    }

    private void setupAdminContext() {
        UserPrincipal principal = new UserPrincipal(ADMIN_ID, "admin", "ADMIN", "active");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
    }

    private void setupUserContext(String userId, String username) {
        UserPrincipal principal = new UserPrincipal(userId, username, "USER", "active");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    private void waitForAsync() {
        try {
            Thread.sleep(2000); // Admin ban has multiple async steps
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Admin: Delete Review of another user")
    void testAdminDeleteReview() {
        // 1. Create review as a normal user
        setupUserContext(USER_ID, "user_to_ban");
        ReviewDTO dto = new ReviewDTO();
        dto.setBookId(BOOK_ID);
        dto.setRating(70);
        ReviewDTO saved = reviewService.createReview(dto);
        createdReviewIds.add(saved.getId());
        
        // 2. Switch to admin and delete it
        setupAdminContext();
        adminModerationService.deleteReview(saved.getId());

        waitForAsync();

        // 3. Verify deletion
        assertFalse(reviewRepository.existsById(saved.getId()));
        
        // Verify stats update
        BookDocument book = bookRepository.findById(BOOK_ID).get();
        assertTrue(book.getStatsPerYear().isEmpty() || book.getStatsPerYear().get(0).getRatingsCount() == 0);
    }

    @Test
    @Order(2)
    @DisplayName("Admin: Ban User and cascading deletes")
    void testBanUserCascading() {
        // 1. Setup user with reviews
        setupUserContext(USER_ID, "user_to_ban");
        
        // Salva il count iniziale per ignorare eventuali review di background
        long initialReviewCount = reviewRepository.count();
        
        ReviewDTO dto = new ReviewDTO();
        dto.setBookId(BOOK_ID);
        dto.setRating(90);
        ReviewDTO created = reviewService.createReview(dto);
        createdReviewIds.add(created.getId());
        
        waitForAsync();
        
        // Verify initial state - verifica che la review sia stata creata
        assertEquals(initialReviewCount + 1, reviewRepository.count(), "One review should be created");
        BookDocument bookBefore = bookRepository.findById(BOOK_ID).get();
        assertEquals(1, bookBefore.getStatsPerYear().get(0).getRatingsCount());

        // 2. Ban user as admin
        setupAdminContext();
        adminModerationService.banUser(USER_ID);

        waitForAsync();

        // 3. Verify
        // User status
        RegisteredUser user = userRepository.findById(USER_ID).get();
        assertEquals("BANNED", user.getStatus());
        assertNull(user.getUsername());

        // Review deletion - verifica che la review sia stata eliminata
        assertEquals(initialReviewCount, reviewRepository.count(), "All reviews of banned user should be deleted");

        // Stats update
        BookDocument bookAfter = bookRepository.findById(BOOK_ID).get();
        assertTrue(bookAfter.getStatsPerYear().isEmpty() || bookAfter.getStatsPerYear().get(0).getRatingsCount() == 0, 
                  "Book stats should be updated after ban-induced review deletion");

        // Neo4j deletion
        assertFalse(userNodeRepository.findByMongoId(USER_ID).isPresent(), "User node should be deleted from Neo4j");
    }

    @Test
    @Order(3)
    @DisplayName("Admin: Ban already banned user")
    void testBanAlreadyBannedUser() {
        // 1. Ban once
        adminModerationService.banUser(USER_ID);
        
        // 2. Try to ban again
        assertThrows(UserAlreadyBannedException.class, () -> adminModerationService.banUser(USER_ID));
    }

    @Test
    @Order(4)
    @DisplayName("Admin: Ban non-existent user")
    void testBanNonExistentUser() {
        assertThrows(UserNotFoundException.class, () -> adminModerationService.banUser("invalid_id_123456789012"));
    }
}
