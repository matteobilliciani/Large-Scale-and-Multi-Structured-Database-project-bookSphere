package it.unipi.bookSphere;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.model.mongodb.*;
import it.unipi.bookSphere.repository.mongo.*;
import it.unipi.bookSphere.repository.neo4j.*;
import it.unipi.bookSphere.service.*;
import it.unipi.bookSphere.utils.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Integration Test for Analytics APIs.
 * Tests: Internationality Index, Genre Influencers, User Recommendations
 * Creates fresh test data for each run.
 */
@SpringBootTest
@ActiveProfiles("clusterWSL")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnalyticsIntegrationTest {

    @Autowired private AnalyticsService analyticsService;
    @Autowired private UserFeaturesService userFeaturesService;
    @Autowired private AuthService authService;
    @Autowired private ReviewService reviewService;
    @Autowired private FollowService followService;
    @Autowired private LikeService likeService;

    @Autowired private BookRepository bookRepository;
    @Autowired private AuthorRepository authorRepository;
    @Autowired private RegisteredUserRepository userRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private BookNodeRepository bookNodeRepository;
    @Autowired private AuthorNodeRepository authorNodeRepository;
    @Autowired private GenreNodeRepository genreNodeRepository;
    @Autowired private UserNodeRepository userNodeRepository;
    // @Autowired private ReviewNodeRepository reviewNodeRepository; // Not used in tests

    private static String testUserId1, testUserId2, testUserId3, testUserId4, testUserId5;
    private static String testBookId1, testBookId2, testBookId3, testBookId4;
    private static String testAuthorId;
    
    private static final String TEST_PREFIX = "AnalyticsTest_";
    private static final String TEST_USERNAME1 = TEST_PREFIX + "Italy";
    private static final String TEST_USERNAME2 = TEST_PREFIX + "USA";
    private static final String TEST_USERNAME3 = TEST_PREFIX + "UK";
    private static final String TEST_USERNAME4 = TEST_PREFIX + "France";
    private static final String TEST_USERNAME5 = TEST_PREFIX + "Germany";
    private static final String TEST_GENRE = TEST_PREFIX + "Genre";
    private static final String TEST_AUTHOR_NAME = TEST_PREFIX + "Author";
    private static final String TEST_PASSWORD = "Pass123!";

    @BeforeAll
    static void setupClass() {
        System.out.println("\n========================================");
        System.out.println("Analytics Integration Test");
        System.out.println("========================================\n");
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Order(1)
    void test01_CleanupOldData() {
        System.out.println("\n--- TEST 1: Cleanup Old Test Data ---");
        
        // Cleanup users
        userRepository.findAll().stream()
            .filter(u -> u.getUsername() != null && u.getUsername().startsWith(TEST_PREFIX))
            .forEach(user -> {
                userRepository.deleteById(user.getId());
                System.out.println("  Deleted user: " + user.getUsername());
            });
        
        // Cleanup reviews  
        reviewRepository.findAll().stream()
            .filter(r -> r.getUsername() != null && r.getUsername().startsWith(TEST_PREFIX))
            .forEach(review -> {
                reviewRepository.deleteById(review.getId());
                System.out.println("  Deleted review: " + review.getId());
            });
        
        // Cleanup books
        bookRepository.findAll().stream()
            .filter(b -> b.getTitle() != null && b.getTitle().startsWith(TEST_PREFIX))
            .forEach(book -> {
                bookRepository.deleteById(book.getId());
                System.out.println("  Deleted book: " + book.getTitle());
            });
        
        // Cleanup authors
        authorRepository.findAll().stream()
            .filter(a -> a.getName() != null && a.getName().startsWith(TEST_PREFIX))
            .forEach(author -> {
                authorRepository.deleteById(author.getId());
                System.out.println("  Deleted author: " + author.getName());
            });
        
        // Cleanup Neo4j
        userNodeRepository.findAll().stream()
            .filter(u -> u.getUsername() != null && u.getUsername().startsWith(TEST_PREFIX))
            .forEach(user -> {
                userNodeRepository.deleteByMongoId(user.getMongoId());
                System.out.println("  Deleted Neo4j user node: " + user.getUsername());
            });
        
        System.out.println("✓ Cleanup complete");
    }

    @Test
    @Order(2)
    void test02_CreateTestUsers() {
        System.out.println("\n--- TEST 2: Create Test Users ---");
        
        testUserId1 = createUser(TEST_USERNAME1, "IT");
        testUserId2 = createUser(TEST_USERNAME2, "US");
        testUserId3 = createUser(TEST_USERNAME3, "GB");
        testUserId4 = createUser(TEST_USERNAME4, "FR");
        testUserId5 = createUser(TEST_USERNAME5, "DE");
        
        assertNotNull(testUserId1);
        assertNotNull(testUserId2);
        assertNotNull(testUserId3);
        assertNotNull(testUserId4);
        assertNotNull(testUserId5);
        
        System.out.println("✓ Created 5 test users");
    }

    @Test
    @Order(3)
    void test03_CreateBooksAndAuthor() {
        System.out.println("\n--- TEST 3: Create Books and Author ---");
        
        // Create author
        AuthorDocument author = new AuthorDocument();
        author.setName(TEST_AUTHOR_NAME);
        author.setStatus("ACTIVE");
        author = authorRepository.save(author);
        testAuthorId = author.getId();
        authorNodeRepository.getOrCreate(testAuthorId, TEST_AUTHOR_NAME);
        System.out.println("  Created author: " + TEST_AUTHOR_NAME);
        
        // Create genre
        genreNodeRepository.getOrCreate(TEST_GENRE);
        System.out.println("  Created genre: " + TEST_GENRE);
        
        // Create 4 books
        testBookId1 = createBook("Book1 - International", 2020);
        testBookId2 = createBook("Book2 - Popular", 2021);
        testBookId3 = createBook("Book3 - Recommended", 2022);
        testBookId4 = createBook("Book4 - Extra", 2023);
        
        assertNotNull(testAuthorId);
        assertNotNull(testBookId1);
        assertNotNull(testBookId2);
        assertNotNull(testBookId3);
        assertNotNull(testBookId4);
        
        System.out.println("✓ Created author and 4 books");
    }

    @Test
    @Order(4)
    void test04_CreateReviewsAndInteractions() {
        System.out.println("\n--- TEST 4: Create Reviews and Interactions ---");
        
        // User 1 (Italy - INFLUENCER): Posts 3 reviews on different books
        auth(testUserId1, TEST_USERNAME1);
        String review1_1 = postReview(testBookId1, "Excellent book! Must read!", 5);
        String review1_2 = postReview(testBookId2, "Amazing story!", 5);
        String review1_3 = postReview(testBookId3, "Brilliant writing!", 5);
        System.out.println("  User1: Posted 3 reviews");
        
        // User 2 (USA): Reviews book1, likes book3, follows user1, likes user1's reviews, likes genre
        auth(testUserId2, TEST_USERNAME2);
        followService.followUser(testUserId1);
        postReview(testBookId1, "Good book!", 4);
        likeService.likeBook(testBookId3);
        likeService.likeGenre(TEST_GENRE);  // Add genre like for recommendations
        likeService.likeReview(review1_1);
        likeService.likeReview(review1_2);
        System.out.println("  User2: Reviewed, followed, liked");
        
        // User 3 (UK): Reviews book2 twice (different), likes author/genre, follows user1, likes user1's reviews
        auth(testUserId3, TEST_USERNAME3);
        followService.followUser(testUserId1);
        likeService.likeAuthor(testAuthorId);
        likeService.likeGenre(TEST_GENRE);
        postReview(testBookId2, "Interesting read!", 4);
        postReview(testBookId4, "Nice book!", 4);
        likeService.likeReview(review1_1);
        likeService.likeReview(review1_2);
        likeService.likeReview(review1_3);
        System.out.println("  User3: 2 reviews, liked author/genre");
        
        // User 4 (France): Reviews book2, likes book3, likes user1's reviews
        auth(testUserId4, TEST_USERNAME4);
        postReview(testBookId2, "Great book!", 5);
        likeService.likeBook(testBookId3);
        likeService.likeReview(review1_1);
        System.out.println("  User4: Reviewed, liked");
        
        // User 5 (Germany): Reviews book3, likes author
        auth(testUserId5, TEST_USERNAME5);
        postReview(testBookId3, "Superb!", 5);
        likeService.likeAuthor(testAuthorId);
        System.out.println("  User5: Reviewed, liked author");
        
        System.out.println("✓ Created 8 reviews and interactions");
    }

    @Test
    @Order(10)
    void test10_BookInternationality() {
        System.out.println("\n--- TEST 10: Book Internationality Index ---");
        
        List<InternationalityDTO> result = analyticsService.calculateInternationality(testBookId1, "BOOK");
        
        System.out.println("  Results:");
        result.forEach(dto -> System.out.println(
            String.format("    %s: %d users, %d interactions", 
                dto.getCountry(), dto.getUniqueUsers(), dto.getTotalInteractions())
        ));
        
        assertNotNull(result);
        assertTrue(result.size() >= 2, "Expected >= 2 countries, got " + result.size());
        
        // Book1 has reviews from IT and US
        assertTrue(result.stream().anyMatch(d -> "IT".equals(d.getCountry())));
        assertTrue(result.stream().anyMatch(d -> "US".equals(d.getCountry())));
        
        for (var dto : result) {
            assertTrue(dto.getUniqueUsers() > 0);
            assertTrue(dto.getTotalInteractions() > 0);
        }
        
        System.out.println("✓ PASSED: Book shows international reach");
    }

    @Test
    @Order(11)
    void test11_AuthorInternationality() {
        System.out.println("\n--- TEST 11: Author Internationality Index ---");
        
        List<InternationalityDTO> result = analyticsService.calculateInternationality(testAuthorId, "AUTHOR");
        
        System.out.println("  Results:");
        result.forEach(dto -> System.out.println(
            String.format("    %s: %d users, %d interactions", 
                dto.getCountry(), dto.getUniqueUsers(), dto.getTotalInteractions())
        ));
        
        assertNotNull(result);
        assertTrue(result.size() >= 4, "Expected >= 4 countries, got " + result.size());
        
        // Author has interactions from IT, US, GB, FR, DE
        assertTrue(result.stream().anyMatch(d -> "IT".equals(d.getCountry())), "Missing IT");
        assertTrue(result.stream().anyMatch(d -> "US".equals(d.getCountry())), "Missing US");
        assertTrue(result.stream().anyMatch(d -> "GB".equals(d.getCountry())), "Missing GB");
        assertTrue(result.stream().anyMatch(d -> "FR".equals(d.getCountry())), "Missing FR");
        
        System.out.println("✓ PASSED: Author has global reach");
    }

    @Test
    @Order(12)
    void test12_GenreInfluencers() {
        System.out.println("\n--- TEST 12: Genre Influencers ---");
        
        List<InfluencerDTO> result = analyticsService.getInfluencers(TEST_GENRE, 10);
        
        System.out.println("  Results:");
        result.forEach(dto -> System.out.println(
            String.format("    %s: engagement=%d, reviews=%d, avg_likes=%.1f", 
                dto.getUsername(), dto.getTotalEngagement(), 
                dto.getNumReviews(), dto.getAvgLikesPerReview())
        ));
        
        assertNotNull(result);
        assertTrue(result.size() > 0, "Expected at least 1 influencer");
        
        // User1 should be top influencer (has 3 reviews with multiple likes each)
        InfluencerDTO top = result.get(0);
        assertEquals(TEST_USERNAME1, top.getUsername(), "Expected User1 to be top influencer");
        assertTrue(top.getNumReviews() >= 2, "Expected >= 2 reviews, got " + top.getNumReviews());
        assertTrue(top.getTotalEngagement() >= 3, "Expected engagement >= 3, got " + top.getTotalEngagement());
        
        System.out.println("✓ PASSED: User1 is top influencer");
    }

    @Test
    @Order(13)
    void test13_TopInfluencers() {
        System.out.println("\n--- TEST 13: Top Influencers (All Genres) ---");
        
        List<InfluencerDTO> result = analyticsService.getInfluencers(null, 5);
        
        System.out.println("  Results:");
        result.forEach(dto -> System.out.println(
            String.format("    %s: engagement=%d, reviews=%d, avg_likes=%.1f", 
                dto.getUsername(), dto.getTotalEngagement(), 
                dto.getNumReviews(), dto.getAvgLikesPerReview())
        ));
        
        assertNotNull(result);
        assertTrue(result.size() > 0, "Expected at least 1 top influencer");
        
        boolean hasUser1 = result.stream()
            .anyMatch(dto -> TEST_USERNAME1.equals(dto.getUsername()));
        assertTrue(hasUser1, "Expected User1 in top influencers");
        
        System.out.println("✓ PASSED: Top influencers identified");
    }

    @Test
    @Order(14)
    void test14_User2Recommendations() {
        System.out.println("\n--- TEST 14: User2 Recommendations ---");
        
        auth(testUserId2, TEST_USERNAME2);
        List<RecommendationDTO> result = userFeaturesService.getRecommendations(10);
        
        System.out.println("  Results:");
        result.forEach(dto -> System.out.println(
            String.format("    '%s' by %s (score=%d, year=%d)", 
                dto.getTitle(), dto.getAuthorName(), 
                dto.getScore(), dto.getPublicationYear())
        ));
        
        assertNotNull(result);
        assertTrue(result.size() > 0, "Expected at least 1 recommendation");
        
        // Should NOT recommend Book1 (already reviewed)
        boolean hasBook1 = result.stream()
            .anyMatch(dto -> testBookId1.equals(dto.getBookId()));
        assertFalse(hasBook1, "Should NOT recommend already reviewed Book1");
        
        System.out.println("✓ PASSED: User2 gets relevant recommendations");
    }

    @Test
    @Order(15)
    void test15_User3Recommendations() {
        System.out.println("\n--- TEST 15: User3 Recommendations ---");
        
        auth(testUserId3, TEST_USERNAME3);
        List<RecommendationDTO> result = userFeaturesService.getRecommendations(10);
        
        System.out.println("  Results:");
        result.forEach(dto -> System.out.println(
            String.format("    '%s' by %s (score=%d, year=%d)", 
                dto.getTitle(), dto.getAuthorName(), 
                dto.getScore(), dto.getPublicationYear())
        ));
        
        assertNotNull(result);
        assertTrue(result.size() > 0, "Expected at least 1 recommendation");
        
        // Should NOT recommend Book2 and Book4 (already reviewed)
        boolean hasBook2 = result.stream()
            .anyMatch(dto -> testBookId2.equals(dto.getBookId()));
        boolean hasBook4 = result.stream()
            .anyMatch(dto -> testBookId4.equals(dto.getBookId()));
        assertFalse(hasBook2, "Should NOT recommend already reviewed Book2");
        assertFalse(hasBook4, "Should NOT recommend already reviewed Book4");
        
        // Should recommend Book1 or Book3 (same author/genre, not reviewed, liked author)
        boolean hasRelevant = result.stream()
            .anyMatch(dto -> testBookId1.equals(dto.getBookId()) || 
                           testBookId3.equals(dto.getBookId()));
        assertTrue(hasRelevant, "Should recommend Book1 or Book3 (same author/genre, user liked author)");
        
        System.out.println("✓ PASSED: User3 gets relevant recommendations");
    }

    // ========== HELPER METHODS ==========
    
    private String createUser(String username, String country) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setPassword(TEST_PASSWORD);
        dto.setEmail(username.toLowerCase() + "@test.com");
        dto.setCountry(country);
        String userId = authService.register(dto).getId();
        System.out.println("  Created user: " + username + " (" + country + ")");
        return userId;
    }

    private String createBook(String titleSuffix, int year) {
        String title = TEST_PREFIX + titleSuffix;
        BookDocument book = new BookDocument();
        book.setTitle(title);
        book.setPublicationYear(year);
        book.setAuthor(new BookDocument.Author(testAuthorId, TEST_AUTHOR_NAME));
        book.setGenres(List.of(TEST_GENRE));
        book.setDescription("Test book for analytics");
        book.setStatus("ACTIVE");
        book = bookRepository.save(book);
        String bookId = book.getId();
        bookNodeRepository.getOrCreate(bookId, title, year);
        authorNodeRepository.createWroteRelationship(testAuthorId, bookId);
        // Normalize genre name for Neo4j relationship
        String normalizedGenre = it.unipi.bookSphere.utils.NormalizationUtils.normalizeGenreName(TEST_GENRE);
        bookNodeRepository.createBelongsToRelationship(bookId, normalizedGenre);
        System.out.println("  Created book: " + title);
        return bookId;
    }

    private void auth(String userId, String username) {
        UserPrincipal principal = new UserPrincipal(userId, username, "USER", "ACTIVE");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, 
                List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    private String postReview(String bookId, String text, int rating) {
        ReviewDTO dto = new ReviewDTO();
        dto.setBookId(bookId);
        dto.setText(text);
        dto.setRating(rating);
        return reviewService.createReview(dto).getId();
    }

    @AfterAll
    static void teardownClass() {
        System.out.println("\n========================================");
        System.out.println("Test Complete");
        System.out.println("========================================\n");
    }
}
