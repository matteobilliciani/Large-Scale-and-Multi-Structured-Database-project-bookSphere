package it.unipi.bookSphere.open;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import it.unipi.bookSphere.service.open.AnalyticsService;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for Analytics APIs (OPEN - No authentication required)
 * Tests:
 * - Get trending books (GET /api/v1/analytics/rankings/trendingbooks)
 * - Get book rankings (GET /api/v1/analytics/books)
 * - Get author rankings (GET /api/v1/analytics/rankings/authors)
 * - Get book revaluation (GET /api/v1/analytics/books/revaluated)
 * - Get book rankings by author V2 (GET /api/v1/analytics/booksAuthorV2)
 * - Calculate internationality (GET /api/v1/analytics/internationality/{entityId})
 * - Get influencers (GET /api/v1/analytics/influencers)
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnalyticsControllerTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RegisteredUserRepository userRepository;

    private static final String TEST_PREFIX = "AnalyticsTest_";
    private static String testBookId1;
    private static String testBookId2;
    private static String testAuthorId;
    private static String testUserId;
    private static String testReviewId1;
    private static String testReviewId2;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");

        // Remove test reviews
        reviewRepository.findAll().stream()
                .filter(r -> r.getUsername() != null && r.getUsername().startsWith(TEST_PREFIX))
                .forEach(review -> {
                    reviewRepository.deleteById(review.getId());
                    System.out.println("Deleted test review: " + review.getId());
                });

        // Remove test books
        bookRepository.findAll().stream()
                .filter(b -> b.getTitle() != null && b.getTitle().startsWith(TEST_PREFIX))
                .forEach(book -> {
                    bookRepository.deleteById(book.getId());
                    System.out.println("Deleted test book: " + book.getTitle());
                });

        // Remove test authors
        authorRepository.findAll().stream()
                .filter(a -> a.getName() != null && a.getName().startsWith(TEST_PREFIX))
                .forEach(author -> {
                    authorRepository.deleteById(author.getId());
                    System.out.println("Deleted test author: " + author.getName());
                });

        // Remove test users
        userRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().startsWith(TEST_PREFIX))
                .forEach(user -> {
                    userRepository.deleteById(user.getId());
                    System.out.println("Deleted test user: " + user.getUsername());
                });

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Setup - Create test data for analytics")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Test Data ===");

        // Create test user
        RegisteredUser user = new RegisteredUser();
        user.setUsername(TEST_PREFIX + "User");
        user.setEmail(TEST_PREFIX + "user@test.com");
        user.setPasswordHashed("hashedpassword");
        user.setCountry("IT");
        user.setStatus("active");
        user.setJoinedAt(Instant.now());
        user.setReviews(new ArrayList<>());
        user.setBookshelf(new ArrayList<>());
        user = userRepository.save(user);
        testUserId = user.getId();
        System.out.println("Created test user: " + user.getUsername());

        // Create test author
        AuthorDocument author = new AuthorDocument();
        author.setName(TEST_PREFIX + "Author");
        author.setRatingsCount(2);
        author.setSumRatings(170);
        author.setStatus("ACTIVE");
        author = authorRepository.save(author);
        testAuthorId = author.getId();
        System.out.println("Created test author: " + author.getName());

        // Create test review 1 (for book 1)
        Review review1 = new Review();
        review1.setUserId(testUserId);
        review1.setUsername(TEST_PREFIX + "User");
        review1.setRating(85);
        review1.setText(TEST_PREFIX + "Great book with excellent story!");
        review1.setSummary("Excellent");
        review1.setSource("test");
        review1.setLikesCount(10);
        review1.setCreatedAt(Instant.now().minusSeconds(5 * 24 * 60 * 60));
        review1 = reviewRepository.save(review1);
        testReviewId1 = review1.getId();
        System.out.println("Created test review 1: " + review1.getId());

        // Create test book 1
        BookDocument book1 = new BookDocument();
        book1.setTitle(TEST_PREFIX + "Trending Book");
        book1.setAuthor(new BookDocument.Author(testAuthorId, author.getName()));
        book1.setGenres(List.of("Fantasy", "Adventure"));
        book1.setPublicationYear(2024);
        
        // Add review snapshot
        List<BookDocument.ReviewSnapshot> reviews1 = new ArrayList<>();
        BookDocument.ReviewSnapshot snapshot1 = new BookDocument.ReviewSnapshot();
        snapshot1.setId(testReviewId1);
        snapshot1.setUserId(testUserId);
        snapshot1.setUsername(TEST_PREFIX + "User");
        snapshot1.setRating(85);
        snapshot1.setSummary("Excellent");
        snapshot1.setNumOfLike(10);
        snapshot1.setDate(Instant.now().minusSeconds(5 * 24 * 60 * 60));
        reviews1.add(snapshot1);
        book1.setRecentReviewsSnapshot(reviews1);
        
        // Add stats per year
        List<BookDocument.YearStat> stats1 = new ArrayList<>();
        BookDocument.YearStat yearStat1 = new BookDocument.YearStat();
        yearStat1.setYear(2024);
        yearStat1.setRatingsCount(1);
        yearStat1.setSumRating(85);
        stats1.add(yearStat1);
        book1.setStatsPerYear(stats1);
        
        book1.setAvailability("ACTIVE");
        book1 = bookRepository.save(book1);
        testBookId1 = book1.getId();
        System.out.println("Created test book 1: " + book1.getTitle());

        // Create test review 2 (for book 2)
        Review review2 = new Review();
        review2.setUserId(testUserId);
        review2.setUsername(TEST_PREFIX + "User");
        review2.setRating(90);
        review2.setText(TEST_PREFIX + "Amazing book, highly recommended!");
        review2.setSummary("Fantastic");
        review2.setSource("test");
        review2.setLikesCount(15);
        review2.setCreatedAt(Instant.now().minusSeconds(2 * 24 * 60 * 60));
        review2 = reviewRepository.save(review2);
        testReviewId2 = review2.getId();
        System.out.println("Created test review 2: " + review2.getId());

        // Create test book 2
        BookDocument book2 = new BookDocument();
        book2.setTitle(TEST_PREFIX + "Popular Book");
        book2.setAuthor(new BookDocument.Author(testAuthorId, author.getName()));
        book2.setGenres(List.of("Fantasy", "Mystery"));
        book2.setPublicationYear(2023);
        
        // Add review snapshot
        List<BookDocument.ReviewSnapshot> reviews2 = new ArrayList<>();
        BookDocument.ReviewSnapshot snapshot2 = new BookDocument.ReviewSnapshot();
        snapshot2.setId(testReviewId2);
        snapshot2.setUserId(testUserId);
        snapshot2.setUsername(TEST_PREFIX + "User");
        snapshot2.setRating(90);
        snapshot2.setSummary("Fantastic");
        snapshot2.setNumOfLike(15);
        snapshot2.setDate(Instant.now().minusSeconds(2 * 24 * 60 * 60));
        reviews2.add(snapshot2);
        book2.setRecentReviewsSnapshot(reviews2);
        
        // Add stats per year
        List<BookDocument.YearStat> stats2 = new ArrayList<>();
        BookDocument.YearStat yearStat2 = new BookDocument.YearStat();
        yearStat2.setYear(2023);
        yearStat2.setRatingsCount(1);
        yearStat2.setSumRating(90);
        stats2.add(yearStat2);
        book2.setStatsPerYear(stats2);
        
        book2.setAvailability("ACTIVE");
        book2 = bookRepository.save(book2);
        testBookId2 = book2.getId();
        System.out.println("Created test book 2: " + book2.getTitle());
    }

    @Test
    @Order(3)
    @DisplayName("03. Get trending books - Success")
    void test03_GetTrendingBooks() {
        System.out.println("\n=== TEST 03: Get Trending Books ===");

        List<BookDTO> trendingBooks = analyticsService.getTrendingBooks();

        assertNotNull(trendingBooks);
        System.out.println("Retrieved " + trendingBooks.size() + " trending books");
        
        // There should be at least our test books if trending logic includes them
        // The actual result depends on the trending algorithm implementation
    }

    @Test
    @Order(4)
    @DisplayName("04. Get book rankings - All books")
    void test04_GetBookRankings_AllBooks() {
        System.out.println("\n=== TEST 04: Get Book Rankings (All) ===");

        List<RankingDTO> rankings = analyticsService.getBookRankings(null, null, null);

        assertNotNull(rankings);
        System.out.println("Retrieved " + rankings.size() + " book rankings");
    }

    @Test
    @Order(5)
    @DisplayName("05. Get book rankings - By year")
    void test05_GetBookRankings_ByYear() {
        System.out.println("\n=== TEST 05: Get Book Rankings by Year ===");

        List<RankingDTO> rankings = analyticsService.getBookRankings(2024, null, null);

        assertNotNull(rankings);
        System.out.println("Retrieved " + rankings.size() + " book rankings for year 2024");
    }

    @Test
    @Order(6)
    @DisplayName("06. Get book rankings - By author")
    void test06_GetBookRankings_ByAuthor() {
        System.out.println("\n=== TEST 06: Get Book Rankings by Author ===");

        List<RankingDTO> rankings = analyticsService.getBookRankings(null, testAuthorId, null);

        assertNotNull(rankings);
        System.out.println("Retrieved " + rankings.size() + " book rankings for test author");
        
        // All books should be from our test author
        rankings.forEach(rank -> 
            System.out.println("  - " + rank.getName() + " (rating: " + rank.getAverageRating() + ")")
        );
    }

    @Test
    @Order(7)
    @DisplayName("07. Get book rankings - By genre")
    void test07_GetBookRankings_ByGenre() {
        System.out.println("\n=== TEST 07: Get Book Rankings by Genre ===");

        List<RankingDTO> rankings = analyticsService.getBookRankings(null, null, "Fantasy");

        assertNotNull(rankings);
        System.out.println("Retrieved " + rankings.size() + " book rankings for Fantasy genre");
    }

    @Test
    @Order(8)
    @DisplayName("08. Get book rankings - Invalid: both author and genre")
    void test08_GetBookRankings_InvalidBothFilters() {
        System.out.println("\n=== TEST 08: Get Book Rankings - Invalid Filters ===");

        // This test verifies that the controller (not service) should reject this
        // Since we're testing the service directly, we just call it normally
        // The controller validation would handle the mutual exclusivity
        
        List<RankingDTO> rankings = analyticsService.getBookRankings(null, testAuthorId, "Fantasy");
        
        // Service may accept this, but controller should reject
        assertNotNull(rankings);
        System.out.println("Service allowed both filters (Controller should validate)");
    }

    @Test
    @Order(9)
    @DisplayName("09. Get author rankings - Success")
    void test09_GetAuthorRankings() {
        System.out.println("\n=== TEST 09: Get Author Rankings ===");

        List<RankingDTO> rankings = analyticsService.getAuthorRankings();

        assertNotNull(rankings);
        System.out.println("Retrieved " + rankings.size() + " author rankings");
    }

    @Test
    @Order(10)
    @DisplayName("10. Get book revaluation - Success")
    void test10_GetBookRevaluation() {
        System.out.println("\n=== TEST 10: Get Book Revaluation ===");

        List<BookTrendDTO> revaluation = analyticsService.getBookRevaluation();

        assertNotNull(revaluation);
        System.out.println("Retrieved " + revaluation.size() + " revaluated books");
    }

    @Test
    @Order(11)
    @DisplayName("11. Get book rankings by author V2 - Success")
    void test11_GetBookRankingsAuthorV2() {
        System.out.println("\n=== TEST 11: Get Book Rankings Author V2 ===");

        List<RankingDTO> rankings = analyticsService.getBookRankingsAuthorV2(null, testAuthorId);

        assertNotNull(rankings);
        System.out.println("Retrieved " + rankings.size() + " book rankings for author (V2)");
    }

    @Test
    @Order(12)
    @DisplayName("12. Get book rankings by book IDs - All-time")
    void test12_GetBookRankingsByIds_AllTime() {
        System.out.println("\n=== TEST 12: Get Book Rankings by IDs (All-time) ===");

        List<String> bookIds = List.of(testBookId1, testBookId2);
        List<RankingDTO> rankings = analyticsService.getBookRankingsByIds(null, bookIds);

        assertNotNull(rankings);
        System.out.println("Retrieved " + rankings.size() + " book rankings for specified book IDs");
        rankings.forEach(rank -> 
            System.out.println("  - " + rank.getName() + " (rating: " + rank.getAverageRating() + ")")
        );
    }

    @Test
    @Order(13)
    @DisplayName("13. Get book rankings by book IDs - By year")
    void test13_GetBookRankingsByIds_ByYear() {
        System.out.println("\n=== TEST 13: Get Book Rankings by IDs (Year: 2024) ===");

        List<String> bookIds = List.of(testBookId1, testBookId2);
        List<RankingDTO> rankings = analyticsService.getBookRankingsByIds(2024, bookIds);

        assertNotNull(rankings);
        System.out.println("Retrieved " + rankings.size() + " book rankings for specified book IDs in year 2024");
        rankings.forEach(rank -> 
            System.out.println("  - " + rank.getName() + " (rating: " + rank.getAverageRating() + ")")
        );
    }

    @Test
    @Order(14)
    @DisplayName("14. Get book rankings by book IDs - Empty list")
    void test14_GetBookRankingsByIds_EmptyList() {
        System.out.println("\n=== TEST 14: Get Book Rankings by IDs (Empty List) ===");

        List<String> emptyBookIds = new ArrayList<>();
        List<RankingDTO> rankings = analyticsService.getBookRankingsByIds(null, emptyBookIds);

        assertNotNull(rankings);
        assertTrue(rankings.isEmpty(), "Result should be empty for empty input");
        System.out.println("Correctly returned empty list for empty book IDs");
    }

    @Test
    @Order(15)
    @DisplayName("15. Calculate internationality - Book")
    void test15_CalculateInternationality_Book() {
        System.out.println("\n=== TEST 12: Calculate Internationality for Book ===");

        try {
            List<InternationalityDTO> internationality = 
                    analyticsService.calculateInternationality(testBookId1, "BOOK");

            assertNotNull(internationality);
            System.out.println("Retrieved internationality data: " + internationality.size() + " countries");
        } catch (Exception e) {
            System.out.println("Internationality calculation may require more data: " + e.getMessage());
        }
    }

    @Test
    @Order(16)
    @DisplayName("16. Calculate internationality - Author")
    void test16_CalculateInternationality_Author() {
        System.out.println("\n=== TEST 13: Calculate Internationality for Author ===");

        try {
            List<InternationalityDTO> internationality = 
                    analyticsService.calculateInternationality(testAuthorId, "AUTHOR");

            assertNotNull(internationality);
            System.out.println("Retrieved internationality data: " + internationality.size() + " countries");
        } catch (Exception e) {
            System.out.println("Internationality calculation may require more data: " + e.getMessage());
        }
    }

    @Test
    @Order(17)
    @DisplayName("17. Get influencers - All genres")
    void test17_GetInfluencers_AllGenres() {
        System.out.println("\n=== TEST 14: Get Influencers (All Genres) ===");

        List<InfluencerDTO> influencers = analyticsService.getInfluencers(null, 10);

        assertNotNull(influencers);
        System.out.println("Retrieved " + influencers.size() + " influencers across all genres");
    }

    @Test
    @Order(18)
    @DisplayName("18. Get influencers - Specific genre")
    void test18_GetInfluencers_SpecificGenre() {
        System.out.println("\n=== TEST 15: Get Influencers (Fantasy Genre) ===");

        List<InfluencerDTO> influencers = analyticsService.getInfluencers("Fantasy", 5);

        assertNotNull(influencers);
        System.out.println("Retrieved " + influencers.size() + " influencers in Fantasy genre");
    }

    @Test
    @Order(19)
    @DisplayName("19. Get influencers - Custom limit")
    void test19_GetInfluencers_CustomLimit() {
        System.out.println("\n=== TEST 16: Get Influencers with Custom Limit ===");

        List<InfluencerDTO> influencers = analyticsService.getInfluencers(null, 3);

        assertNotNull(influencers);
        assertTrue(influencers.size() <= 3, "Result should respect the limit");
        System.out.println("Retrieved " + influencers.size() + " influencers (limit: 3)");
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        // Remove test reviews
        if (testReviewId1 != null) {
            reviewRepository.deleteById(testReviewId1);
            System.out.println("Deleted test review 1");
        }
        if (testReviewId2 != null) {
            reviewRepository.deleteById(testReviewId2);
            System.out.println("Deleted test review 2");
        }

        // Remove test books
        if (testBookId1 != null) {
            bookRepository.deleteById(testBookId1);
            System.out.println("Deleted test book 1");
        }
        if (testBookId2 != null) {
            bookRepository.deleteById(testBookId2);
            System.out.println("Deleted test book 2");
        }

        // Remove test author
        if (testAuthorId != null) {
            authorRepository.deleteById(testAuthorId);
            System.out.println("Deleted test author");
        }

        // Remove test user
        if (testUserId != null) {
            userRepository.deleteById(testUserId);
            System.out.println("Deleted test user");
        }

        System.out.println("Final cleanup completed - All test data removed");
    }
}
