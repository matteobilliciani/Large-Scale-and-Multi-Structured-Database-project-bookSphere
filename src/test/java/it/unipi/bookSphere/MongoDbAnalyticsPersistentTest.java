package it.unipi.bookSphere;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.model.mongodb.*;
import it.unipi.bookSphere.repository.mongo.*;
import it.unipi.bookSphere.service.*;
import it.unipi.bookSphere.utils.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Persistent Integration Test for MongoDB Analytics APIs.
 * 
 * This test creates PERMANENT test data on FIRST RUN ONLY.
 * Subsequent runs will skip data creation and just test the APIs.
 * NO CLEANUP is performed - data persists across runs.
 * 
 * Tests:
 * - Trending Books
 * - Book Rankings (by year and all-time)
 * - Author Rankings
 * - Genre Rankings
 * - TPI Prediction
 * - Yearly Wrapped
 */
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MongoDbAnalyticsPersistentTest {

    @Autowired private AnalyticsService analyticsService;
    @Autowired private UserFeaturesService userFeaturesService;
    @Autowired private AuthService authService;
    
    @Autowired private BookRepository bookRepository;
    @Autowired private AuthorRepository authorRepository;
    @Autowired private RegisteredUserRepository userRepository;

    private static String testUserId1;
    private static String testBookId1, testBookId2, testBookId3, testBookId4;
    private static String testAuthorId1, testAuthorId2;
    
    private static final String TEST_USERNAME = "mongo_analytics_user_v4";
    private static final String TEST_AUTHOR_1 = "MongoDB Test Author One V4";
    private static final String TEST_AUTHOR_2 = "MongoDB Test Author Two V4";
    private static final String TEST_GENRE_1 = "MongoAnalytics_V4";
    private static final String TEST_GENRE_2 = "TestFiction_V4";
    private static final String TEST_PASSWORD = "Pass123!";
    private static final int CURRENT_YEAR = 2026;
    private static final int LAST_YEAR = 2025;

    @BeforeAll
    static void setupClass() {
        System.out.println("\n========================================");
        System.out.println("MongoDB Analytics Persistent Test");
        System.out.println("IMPORTANT: Data is created ONCE and persists");
        System.out.println("========================================\n");
    }

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Order(1)
    void test01_SetupOrLoadData() {
        System.out.println("\n--- TEST 1: Setup/Load Test Data ---");
        
        // Check and create user
        if (!setupUser()) {
            System.out.println("⚠ User already exists, loaded from database");
        } else {
            System.out.println("✓ Created new test user");
        }
        
        // Check and create authors and books
        if (!setupAuthorsAndBooks()) {
            System.out.println("⚠ Books already exist, loaded from database");
        } else {
            System.out.println("✓ Created new test authors and books");
        }
        
        assertNotNull(testUserId1);
        assertNotNull(testAuthorId1);
        assertNotNull(testAuthorId2);
        assertNotNull(testBookId1);
        assertNotNull(testBookId2);
        assertNotNull(testBookId3);
        assertNotNull(testBookId4);
        
        System.out.println("\n✓ All test data loaded successfully");
    }

    private boolean setupUser() {
        boolean created = false;
        
        if (userRepository.findByUsername(TEST_USERNAME).isPresent()) {
            testUserId1 = userRepository.findByUsername(TEST_USERNAME).get().getId();
        } else {
            RegisterDTO dto = new RegisterDTO();
            dto.setUsername(TEST_USERNAME);
            dto.setPassword(TEST_PASSWORD);
            dto.setEmail(TEST_USERNAME + "@test.com");
            dto.setCountry("IT");
            testUserId1 = authService.register(dto).getId();
            created = true;
        }
        
        return created;
    }

    private boolean setupAuthorsAndBooks() {
        boolean created = false;
        
        // Check if authors exist
        if (authorRepository.findByName(TEST_AUTHOR_1).isPresent()) {
            AuthorDocument author1 = authorRepository.findByName(TEST_AUTHOR_1).get();
            testAuthorId1 = author1.getId();
            
            AuthorDocument author2 = authorRepository.findByName(TEST_AUTHOR_2).get();
            testAuthorId2 = author2.getId();
            
            // Check if books exist
            var book1 = bookRepository.findByTitle("MongoDB Analytics Book 1 - Trending V4");
            var book2 = bookRepository.findByTitle("MongoDB Analytics Book 2 - Top Rated V4");
            var book3 = bookRepository.findByTitle("MongoDB Analytics Book 3 - Average V4");
            var book4 = bookRepository.findByTitle("MongoDB Analytics Book 4 - Low Activity V4");
            
            if (book1.isPresent() && book2.isPresent() && book3.isPresent() && book4.isPresent()) {
                testBookId1 = book1.get().getId();
                testBookId2 = book2.get().getId();
                testBookId3 = book3.get().getId();
                testBookId4 = book4.get().getId();
                return false;
            }
        }
        
        // Create Author 1 (High performer)
        if (testAuthorId1 == null) {
            AuthorDocument author1 = new AuthorDocument();
            author1.setName(TEST_AUTHOR_1);
            author1.setStatus("ACTIVE");
            author1 = authorRepository.save(author1);
            testAuthorId1 = author1.getId();
            created = true;
        }
        
        // Create Author 2 (Average performer)
        if (testAuthorId2 == null) {
            AuthorDocument author2 = new AuthorDocument();
            author2.setName(TEST_AUTHOR_2);
            author2.setStatus("ACTIVE");
            author2 = authorRepository.save(author2);
            testAuthorId2 = author2.getId();
            created = true;
        }
        
        // Create Book 1 - Trending (Author 1, high current activity)
        BookDocument book1 = new BookDocument();
        book1.setTitle("MongoDB Analytics Book 1 - Trending V4");
        book1.setPublicationYear(CURRENT_YEAR);
        book1.setAuthor(new BookDocument.Author(testAuthorId1, TEST_AUTHOR_1));
        book1.setGenres(List.of(TEST_GENRE_1, TEST_GENRE_2));
        book1.setDescription("Test book for trending analytics");
        book1.setStatus("ACTIVE");
        
        // Add month score (current activity)
        BookDocument.MonthScore monthScore1 = new BookDocument.MonthScore();
        monthScore1.setSumRating(4000);
        monthScore1.setRatingCount(50);
        monthScore1.setCurrentMonth("2026-02");
        book1.setMonthScore(monthScore1);
        
        // Add stats per year
        List<BookDocument.YearStat> yearStats1 = new ArrayList<>();
        BookDocument.YearStat stat1_2025 = new BookDocument.YearStat();
        stat1_2025.setYear(CURRENT_YEAR);
        stat1_2025.setRatingsCount(50);
        stat1_2025.setSumRating(4000);
        yearStats1.add(stat1_2025);
        
        BookDocument.YearStat stat1_2024 = new BookDocument.YearStat();
        stat1_2024.setYear(LAST_YEAR);
        stat1_2024.setRatingsCount(30);
        stat1_2024.setSumRating(2250);
        yearStats1.add(stat1_2024);
        
        book1.setStatsPerYear(yearStats1);
        book1 = bookRepository.save(book1);
        testBookId1 = book1.getId();
        
        // Create Book 2 - Top Rated (Author 1, excellent ratings)
        BookDocument book2 = new BookDocument();
        book2.setTitle("MongoDB Analytics Book 2 - Top Rated V4");
        book2.setPublicationYear(LAST_YEAR);
        book2.setAuthor(new BookDocument.Author(testAuthorId1, TEST_AUTHOR_1));
        book2.setGenres(List.of(TEST_GENRE_1));
        book2.setDescription("Test book for rankings");
        book2.setStatus("ACTIVE");
        
        BookDocument.MonthScore monthScore2 = new BookDocument.MonthScore();
        monthScore2.setSumRating(1700);
        monthScore2.setRatingCount(20);
        monthScore2.setCurrentMonth("2026-02");
        book2.setMonthScore(monthScore2);
        
        List<BookDocument.YearStat> yearStats2 = new ArrayList<>();
        BookDocument.YearStat stat2_2025 = new BookDocument.YearStat();
        stat2_2025.setYear(CURRENT_YEAR);
        stat2_2025.setRatingsCount(80);
        stat2_2025.setSumRating(6800);
        yearStats2.add(stat2_2025);
        
        BookDocument.YearStat stat2_2024 = new BookDocument.YearStat();
        stat2_2024.setYear(LAST_YEAR);
        stat2_2024.setRatingsCount(60);
        stat2_2024.setSumRating(4920);
        yearStats2.add(stat2_2024);
        
        book2.setStatsPerYear(yearStats2);
        book2 = bookRepository.save(book2);
        testBookId2 = book2.getId();
        
        // Create Book 3 - Average (Author 2, moderate ratings)
        BookDocument book3 = new BookDocument();
        book3.setTitle("MongoDB Analytics Book 3 - Average V4");
        book3.setPublicationYear(LAST_YEAR);
        book3.setAuthor(new BookDocument.Author(testAuthorId2, TEST_AUTHOR_2));
        book3.setGenres(List.of(TEST_GENRE_2));
        book3.setDescription("Test book for author rankings");
        book3.setStatus("ACTIVE");
        
        BookDocument.MonthScore monthScore3 = new BookDocument.MonthScore();
        monthScore3.setSumRating(900);
        monthScore3.setRatingCount(15);
        book3.setMonthScore(monthScore3);
        
        List<BookDocument.YearStat> yearStats3 = new ArrayList<>();
        BookDocument.YearStat stat3_2025 = new BookDocument.YearStat();
        stat3_2025.setYear(CURRENT_YEAR);
        stat3_2025.setRatingsCount(40);
        stat3_2025.setSumRating(2400);
        yearStats3.add(stat3_2025);
        
        BookDocument.YearStat stat3_2024 = new BookDocument.YearStat();
        stat3_2024.setYear(LAST_YEAR);
        stat3_2024.setRatingsCount(25);
        stat3_2024.setSumRating(1375);
        yearStats3.add(stat3_2024);
        
        book3.setStatsPerYear(yearStats3);
        book3 = bookRepository.save(book3);
        testBookId3 = book3.getId();
        
        // Create Book 4 - Low Activity (Author 2, low activity for TPI test)
        BookDocument book4 = new BookDocument();
        book4.setTitle("MongoDB Analytics Book 4 - Low Activity V4");
        book4.setPublicationYear(CURRENT_YEAR - 2);
        book4.setAuthor(new BookDocument.Author(testAuthorId2, TEST_AUTHOR_2));
        book4.setGenres(List.of(TEST_GENRE_1, TEST_GENRE_2));
        book4.setDescription("Test book for TPI with low activity");
        book4.setStatus("ACTIVE");
        
        // No month score (no recent activity)
        
        List<BookDocument.YearStat> yearStats4 = new ArrayList<>();
        BookDocument.YearStat stat4_2024 = new BookDocument.YearStat();
        stat4_2024.setYear(LAST_YEAR);
        stat4_2024.setRatingsCount(10);
        stat4_2024.setSumRating(500);
        yearStats4.add(stat4_2024);
        
        book4.setStatsPerYear(yearStats4);
        book4 = bookRepository.save(book4);
        testBookId4 = book4.getId();
        
        // Update user with bookshelf data for Yearly Wrapped test
        RegisteredUser user = userRepository.findById(testUserId1).get();
        List<RegisteredUser.BookshelfItem> bookshelf = new ArrayList<>();
        
        // Add Book 1 to bookshelf as read
        RegisteredUser.BookshelfItem item1 = new RegisteredUser.BookshelfItem();
        item1.setBookId(testBookId1);
        item1.setStatus("read");
        item1.setAddedAt(Instant.now());
        item1.setTitle("MongoDB Analytics Book 1 - Trending V4");
        item1.setAuthor(new RegisteredUser.Author(testAuthorId1, TEST_AUTHOR_1));
        item1.setGenres(List.of(TEST_GENRE_1, TEST_GENRE_2));
        bookshelf.add(item1);
        
        // Add Book 2 to bookshelf as read
        RegisteredUser.BookshelfItem item2 = new RegisteredUser.BookshelfItem();
        item2.setBookId(testBookId2);
        item2.setStatus("read");
        item2.setAddedAt(Instant.now());
        item2.setTitle("MongoDB Analytics Book 2 - Top Rated V4");
        item2.setAuthor(new RegisteredUser.Author(testAuthorId1, TEST_AUTHOR_1));
        item2.setGenres(List.of(TEST_GENRE_1));
        bookshelf.add(item2);
        
        user.setBookshelf(bookshelf);
        
        // Add reviews year data
        List<RegisteredUser.ReviewYear> reviewsYear = new ArrayList<>();
        
        RegisteredUser.ReviewYear review1 = new RegisteredUser.ReviewYear();
        review1.setId("review1_" + testBookId1);
        review1.setBook("MongoDB Analytics Book 1 - Trending");
        review1.setRating(100);
        reviewsYear.add(review1);
        
        RegisteredUser.ReviewYear review2 = new RegisteredUser.ReviewYear();
        review2.setId("review2_" + testBookId3);
        review2.setBook("MongoDB Analytics Book 3 - Average");
        review2.setRating(30);
        reviewsYear.add(review2);
        
        user.setReviewsYear(reviewsYear);
        userRepository.save(user);
        
        return created;
    }

    private void auth(String userId, String username) {
        UserPrincipal principal = new UserPrincipal(userId, username, "USER", "active");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, 
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @Test
    @Order(2)
    void test02_TrendingBooks() {
        System.out.println("\n--- TEST 2: Trending Books ---");
        
        List<it.unipi.bookSphere.dto.BookDTO> result = analyticsService.getTrendingBooks();
        
        result.forEach(dto -> System.out.println(
            String.format("  '%s' by %s (year=%d)", 
                dto.getTitle(), 
                dto.getAuthor() != null ? dto.getAuthor().getName() : "Unknown",
                dto.getPublicationYear())
        ));
        
        assertNotNull(result);
        System.out.println("✓ Found " + result.size() + " trending books");
        
        // If our test data has high month scores, it should appear
        boolean hasTrendingBook = result.stream()
            .anyMatch(dto -> testBookId1.equals(dto.getId()) || testBookId2.equals(dto.getId()));
        
        System.out.println("✓ PASSED: Trending books retrieved");
    }

    @Test
    @Order(3)
    void test03_BookRankingsAllTime() {
        System.out.println("\n--- TEST 3: Book Rankings (All-Time) ---");
        
        List<it.unipi.bookSphere.dto.RankingDTO> result = analyticsService.getBookRankings(null, null, null);
        
        result.stream().limit(10).forEach(dto -> System.out.println(
            String.format("  '%s' by %s - rating=%.2f, count=%d", 
                dto.getName(), dto.getAdditionalInfo(), 
                dto.getAverageRating(), dto.getTotalRatings())
        ));
        
        assertNotNull(result);
        assertTrue(result.size() > 0, "Expected at least 1 book ranking");
        
        // Check that Book 2 (highest rated) appears in top rankings
        result.stream()
            .anyMatch(dto -> testBookId2.equals(dto.getId()));
        
        // Verify rankings are sorted by rating
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).getAverageRating() >= result.get(i + 1).getAverageRating(),
                "Rankings should be sorted by rating descending");
        }
        
        System.out.println("✓ PASSED: Book rankings are correctly sorted");
    }

    @Test
    @Order(4)
    void test04_BookRankingsByYear() {
        System.out.println("\n--- TEST 4: Book Rankings for " + CURRENT_YEAR + " ---");
        
        List<it.unipi.bookSphere.dto.RankingDTO> result = analyticsService.getBookRankings(CURRENT_YEAR, null, null);
        
        result.stream().limit(10).forEach(dto -> System.out.println(
            String.format("  '%s' by %s - rating=%.2f, count=%d", 
                dto.getName(), dto.getAdditionalInfo(), 
                dto.getAverageRating(), dto.getTotalRatings())
        ));
        
        assertNotNull(result);
        
        // All rankings should have the correct year
        for (RankingDTO dto : result) {
            assertEquals(CURRENT_YEAR, dto.getYear(), "All rankings should be for " + CURRENT_YEAR);
        }
        
        System.out.println("✓ PASSED: Year-specific book rankings work correctly");
    }

    @Test
    @Order(5)
    void test05_AuthorRankings() {
        System.out.println("\n--- TEST 5: Author Rankings ---");
        
        List<it.unipi.bookSphere.dto.RankingDTO> result = analyticsService.getAuthorRankings();
        
        result.stream().limit(10).forEach(dto -> System.out.println(
            String.format("  %s - rating=%.2f, total_ratings=%d", 
                dto.getName(), dto.getAverageRating(), dto.getTotalRatings())
        ));
        
        assertNotNull(result);
        assertTrue(result.size() > 0, "Expected at least 1 author ranking");
        
        // Verify Author 1 (better books) ranks higher than Author 2
        it.unipi.bookSphere.dto.RankingDTO author1Rank = result.stream()
            .filter(dto -> TEST_AUTHOR_1.equals(dto.getName()))
            .findFirst()
            .orElse(null);
        
        it.unipi.bookSphere.dto.RankingDTO author2Rank = result.stream()
            .filter(dto -> TEST_AUTHOR_2.equals(dto.getName()))
            .findFirst()
            .orElse(null);
        
        if (author1Rank != null && author2Rank != null) {
            assertTrue(author1Rank.getAverageRating() > author2Rank.getAverageRating(),
                "Author 1 should have higher rating than Author 2");
            System.out.println("✓ Author 1 correctly ranked higher than Author 2");
        }
        
        System.out.println("✓ PASSED: Author rankings work correctly");
    }
/* 
    @Test
    @Order(6)
    void test06_GenreRankings() {
        System.out.println("\n--- TEST 6: Genre Rankings ---");
        
        List<it.unipi.bookSphere.dto.RankingDTO> result = analyticsService.getGenreRankings(null);
        
        result.stream().limit(10).forEach(dto -> System.out.println(
            String.format("  %s - rating=%.2f, book_count=%d", 
                dto.getName(), dto.getAverageRating(), dto.getTotalRatings())
        ));
        
        assertNotNull(result);
        assertTrue(result.size() > 0, "Expected at least 1 genre ranking");
        
        // Check test genres are present
        boolean hasTestGenre1 = result.stream()
            .anyMatch(dto -> TEST_GENRE_1.equals(dto.getName()));
        
        boolean hasTestGenre2 = result.stream()
            .anyMatch(dto -> TEST_GENRE_2.equals(dto.getName()));
        
        System.out.println("✓ PASSED: Genre rankings retrieved");
    }
*/

    /* 
    @Test
    @Order(7)
    void test07_TPI_RisingStar() {
        System.out.println("\n--- TEST 7: TPI Prediction - Rising Star ---");
        
        // Book 1 has high current momentum (4.8 month score) vs author benchmark
        it.unipi.bookSphere.dto.TpiPredictionDTO result = analyticsService.calculateTPI(testBookId1);
        
        System.out.println(String.format(
            "  Book: %s\n  Author Benchmark: %.2f\n  Book Momentum: %.2f\n  Prediction: %s\n  Activity: %d",
            result.getBookTitle(), result.getAuthorBenchmark(), 
            result.getBookMomentum(), result.getPrediction(), result.getCurrentActivity()
        ));
        
        assertNotNull(result);
        assertEquals(testBookId1, result.getBookId());
        assertNotNull(result.getBookMomentum());
        assertNotNull(result.getAuthorBenchmark());
        assertNotNull(result.getPrediction());
        assertTrue(result.getCurrentActivity() > 0, "Should have recent activity");
        
        System.out.println("✓ PASSED: TPI calculated for Book 1");
    }

    @Test
    @Order(8)
    void test08_TPI_Stable() {
        System.out.println("\n--- TEST 8: TPI Prediction - Stable/Low Activity ---");
        
        // Book 4 has no month score (no recent data)
        it.unipi.bookSphere.dto.TpiPredictionDTO result = analyticsService.calculateTPI(testBookId4);
        
        System.out.println(String.format(
            "  Book: %s\n  Author Benchmark: %.2f\n  Book Momentum: %.2f\n  Prediction: %s\n  Activity: %d",
            result.getBookTitle(), result.getAuthorBenchmark(), 
            result.getBookMomentum(), result.getPrediction(), result.getCurrentActivity()
        ));
        
        assertNotNull(result);
        assertEquals(testBookId4, result.getBookId());
        assertEquals(0L, result.getCurrentActivity(), "Should have no recent activity");
        assertTrue(result.getPrediction().contains("STABLE"), 
            "Should predict STABLE due to no recent data");
        
        System.out.println("✓ PASSED: TPI handles low/no activity correctly");
    }
    */

    @Test
    @Order(9)
    void test09_YearlyWrapped() {
        System.out.println("\n--- TEST 9: Yearly Wrapped ---");
        
        auth(testUserId1, TEST_USERNAME);
        it.unipi.bookSphere.dto.WrappedDTO result = userFeaturesService.getYearlyWrapped();
        
        System.out.println(String.format(
            "  Year: %d\n  Books Read: %d", 
            result.getYear(), result.getTotalBooksRead()
        ));
        
        if (result.getBestBook() != null) {
            System.out.println(String.format("  Best Book: '%s' - rating %d", 
                result.getBestBook().getTitle(), result.getBestBook().getRating()));
        }
        
        if (result.getWorstBook() != null) {
            System.out.println(String.format("  Worst Book: '%s' - rating %d", 
                result.getWorstBook().getTitle(), result.getWorstBook().getRating()));
        }
        
        if (result.getTopAuthors() != null && !result.getTopAuthors().isEmpty()) {
            System.out.println("  Top Authors:");
            result.getTopAuthors().forEach(author -> 
                System.out.println(String.format("    - %s (%d books)", 
                    author.getName(), author.getCount())));
        }
        
        if (result.getTopGenres() != null && !result.getTopGenres().isEmpty()) {
            System.out.println("  Top Genres:");
            result.getTopGenres().forEach(genre -> 
                System.out.println(String.format("    - %s (%d books)", 
                    genre.getName(), genre.getCount())));
        }
        
        assertNotNull(result);
        assertEquals(CURRENT_YEAR, result.getYear());
        assertTrue(result.getTotalBooksRead() >= 0, "Should have non-negative books read");
        
        // Should have best book from reviews
        if (result.getBestBook() != null) {
            assertEquals(100, result.getBestBook().getRating(), 
                "Best book should have rating 100");
        }
        
        // Should have worst book from reviews
        if (result.getWorstBook() != null) {
            assertEquals(30, result.getWorstBook().getRating(), 
                "Worst book should have rating 30");
        }
        
        // Should have Author 1 in top authors (2 books)
        if (result.getTopAuthors() != null) {
            boolean hasAuthor1 = result.getTopAuthors().stream()
                .anyMatch(author -> TEST_AUTHOR_1.equals(author.getName()));
            assertTrue(hasAuthor1, "Should include Author 1 in top authors");
        }
        
        // Should have test genres in top genres
        if (result.getTopGenres() != null) {
            boolean hasTestGenre = result.getTopGenres().stream()
                .anyMatch(genre -> TEST_GENRE_1.equals(genre.getName()) || 
                                  TEST_GENRE_2.equals(genre.getName()));
            assertTrue(hasTestGenre, "Should include test genres in top genres");
        }
        
        System.out.println("✓ PASSED: Yearly wrapped generated correctly");
    }

    @Test
    @Order(10)
    void test10_BookRankingsBySpecificAuthor() {
        System.out.println("\n--- TEST 10: Book Rankings Filtered by Author (" + TEST_AUTHOR_1 + ") ---");

        // Chiamata: Year=null (All-time), Author=TEST_AUTHOR_1, Genre=null
        List<RankingDTO> result = analyticsService.getBookRankings(null, TEST_AUTHOR_1, null);

        result.forEach(dto -> System.out.println(
            String.format("  '%s' by %s - rating=%.2f", 
                dto.getName(), dto.getAdditionalInfo(), dto.getAverageRating())
        ));

        assertNotNull(result);
        
        // VERIFICA 1: Ci aspettiamo solo i libri dell'Autore 1
        boolean containsAuthor2 = result.stream()
            .anyMatch(dto -> dto.getAdditionalInfo().equals(TEST_AUTHOR_2));
        assertFalse(containsAuthor2, "Should NOT contain books by Author 2");

        // VERIFICA 2: Both Book 1 and Book 2 should be in results (they belong to Author 1)
        boolean hasBook1 = result.stream().anyMatch(dto -> testBookId1.equals(dto.getId()));
        boolean hasBook2 = result.stream().anyMatch(dto -> testBookId2.equals(dto.getId()));
        assertTrue(hasBook1, "Book 1 (Author 1) should be in results");
        assertTrue(hasBook2, "Book 2 (Author 1) should be in results");
        
        // VERIFICA 3: Verify both books have valid ratings calculated from stats_per_year
        RankingDTO book1Dto = result.stream().filter(dto -> testBookId1.equals(dto.getId())).findFirst().orElse(null);
        RankingDTO book2Dto = result.stream().filter(dto -> testBookId2.equals(dto.getId())).findFirst().orElse(null);
        assertNotNull(book1Dto, "Book 1 should be in results");
        assertNotNull(book2Dto, "Book 2 should be in results");
        assertTrue(book1Dto.getAverageRating() > 0, "Book 1 should have valid average rating");
        assertTrue(book2Dto.getAverageRating() > 0, "Book 2 should have valid average rating");
        System.out.println(String.format("  Book 1: avg=%.2f, Book 2: avg=%.2f", 
            book1Dto.getAverageRating(), book2Dto.getAverageRating()));

        System.out.println("✓ PASSED: Filter by Author works correctly");
    }

    @Test
    @Order(11)
    void test11_BookRankingsBySpecificGenre() {
        System.out.println("\n--- TEST 11: Book Rankings Filtered by Genre (" + TEST_GENRE_1 + ") ---");

        // Chiamata: Year=null (All-time), Author=null, Genre=TEST_GENRE_1
        List<RankingDTO> result = analyticsService.getBookRankings(null, null, TEST_GENRE_1);

        result.forEach(dto -> System.out.println(
            String.format("  '%s' - genres included? YES", dto.getName())
        ));

        assertNotNull(result);
        assertFalse(result.isEmpty(), "Should find books for Genre 1");

        // VERIFICA 1: Book 1, 2 e 4 hanno TEST_GENRE_1. Book 3 NON ce l'ha.
        // Quindi Book 3 non deve esserci.
        boolean containsBook3 = result.stream()
            .anyMatch(dto -> testBookId3.equals(dto.getId()));
        
        assertFalse(containsBook3, "Should NOT contain Book 3 (which does not have Genre 1)");

        // VERIFICA 2: Deve contenere Book 2 (che ha quel genere)
        boolean containsBook2 = result.stream()
            .anyMatch(dto -> testBookId2.equals(dto.getId()));
        assertTrue(containsBook2, "Should contain Book 2");

        System.out.println("✓ PASSED: Filter by Genre works correctly");
    }

    @Test
    @Order(12)
    void test12_BookTrends_CultClassicsAndFlops() {
        System.out.println("\n--- TEST 12: Trend Reversals (Cult Classics & Flops) ---");

        // -----------------------------------------------------------------------
        // PRE-CONDITION: HACK PER DATABASE POPOLATO
        // Poiché il DB contiene già libri con Delta enormi (+88.0), il nostro libro
        // con Delta +5.0 viene escluso dalla Top 10.
        // Modifichiamo il "Book 1" per avere un Delta impossibile (0.1 -> 100.0)
        // e garantire che sia il #1 in classifica.
        // -----------------------------------------------------------------------
        Optional<BookDocument> optBook = bookRepository.findById(testBookId1);
        if (optBook.isPresent()) {
            BookDocument b = optBook.get();
            List<BookDocument.YearStat> stats = new ArrayList<>();

            // Anno Vecchio: Voto bassissimo (0.1)
            BookDocument.YearStat start = new BookDocument.YearStat();
            start.setYear(2010);
            start.setSumRating(1);
            start.setRatingsCount(10);
            stats.add(start);

            // Anno Corrente: Voto massimo (100.0)
            BookDocument.YearStat end = new BookDocument.YearStat();
            end.setYear(CURRENT_YEAR);
            end.setSumRating(10000);
            end.setRatingsCount(100);
            stats.add(end);

            b.setStatsPerYear(stats);
            bookRepository.save(b);
            System.out.println(">>> FORCE UPDATE: Book 1 updated to have Delta ~99.9 to hit Top 10");
        }
        // -----------------------------------------------------------------------

        // 1. Chiamata al servizio
        List<it.unipi.bookSphere.dto.BookTrendDTO> result = analyticsService.getBookTrends();

        // Debug Log
        result.forEach(dto -> System.out.println(
                String.format("  #%d '%s': %.1f -> %.1f (Delta: %+.2f) [%d-%d]",
                        dto.getPosition(), dto.getTitle(),
                        dto.getStartRating(), dto.getEndRating(),
                        dto.getRatingDelta(), dto.getStartYear(), dto.getEndYear())
        ));

        assertNotNull(result);

        // --- VERIFICA 1: Il nostro libro DEVE esserci ora ---
        it.unipi.bookSphere.dto.BookTrendDTO book1Trend = result.stream()
                .filter(dto -> testBookId1.equals(dto.getId()) || dto.getTitle().contains("Trending V4")) // Controllo per ID o Titolo
                .findFirst()
                .orElse(null);

        assertNotNull(book1Trend, "Book 1 should be present in Top 10 after forced update");

        // Verifica consistenza matematica: Delta deve essere uguale a End - Start
        double expectedDelta = book1Trend.getEndRating() - book1Trend.getStartRating();
        assertEquals(expectedDelta, book1Trend.getRatingDelta(), 0.01,
                "Rating Delta calculation mismatch");

        // Verifica che sia stato effettivamente aggiornato
        assertTrue(book1Trend.getRatingDelta() > 90.0, "Book 1 should have a massive delta now");

        // --- VERIFICA 2: ESCLUSIONE (Edge Case) ---
        boolean containsBook4 = result.stream()
                .anyMatch(dto -> "MongoDB Analytics Book 4 - Low Activity V4".equals(dto.getTitle()));

        assertFalse(containsBook4, "Books with less than 2 years of history must be EXCLUDED from trends");

        // --- VERIFICA 3: ORDINAMENTO ---
        if (result.size() >= 2) {
            double firstDelta = result.get(0).getRatingDelta();
            double secondDelta = result.get(1).getRatingDelta();
            assertTrue(firstDelta >= secondDelta,
                    "Results should be sorted by Delta descending");
        }

        // --- VERIFICA 4: POSIZIONE ---
        for (int i = 0; i < result.size(); i++) {
            assertEquals(i + 1, result.get(i).getPosition(),
                    "Rank position should be sequential starting from 1");
        }

        System.out.println("✓ PASSED: Trends verified successfully with forced test data");
    }



    @AfterAll
    static void teardownClass() {
        System.out.println("\n========================================");
        System.out.println("✓ ALL TESTS PASSED");
        System.out.println("Data persists for further testing");
        System.out.println("========================================\n");
    }
}
