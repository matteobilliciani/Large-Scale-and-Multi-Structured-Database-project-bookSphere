package it.unipi.bookSphere;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.exceptions.*;
import it.unipi.bookSphere.model.mongodb.*;
import it.unipi.bookSphere.repository.mongo.*;
import it.unipi.bookSphere.service.ReviewService;
import it.unipi.bookSphere.utils.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private AuthorRepository authorRepository;

    private static final String USER_ID = "test_user_id_123";
    private static final String OTHER_USER_ID = "other_user_id_456";
    private static final String BOOK_ID = "test_book_id_789";
    private static final String ARCHIVED_BOOK_ID = "archived_book_id_000";
    private static final String AUTHOR_ID = "test_author_id_abc";

    @Autowired
    private it.unipi.bookSphere.repository.neo4j.ReviewNodeRepository reviewNodeRepository;

    // Tracciamento dei dati creati per il cleanup isolato
    private List<String> createdReviewIds = new ArrayList<>();
    private List<String> createdUserIds = new ArrayList<>();
    private List<String> createdBookIds = new ArrayList<>();
    private List<String> createdAuthorIds = new ArrayList<>();

    private void setupNeo4jReview(String reviewId, Integer rating) {
        it.unipi.bookSphere.model.neo4j.ReviewNode node = new it.unipi.bookSphere.model.neo4j.ReviewNode();
        node.setMongoId(reviewId);
        node.setRating(rating);
        node.setCreatedAt(LocalDateTime.now());
        reviewNodeRepository.save(node);
    }

    @BeforeEach
    void setUp() {
        // Mock authentication for USER_ID
        setupUserContext(USER_ID, "testuser");
        
        // Resetta liste di tracciamento
        createdReviewIds.clear();
        createdUserIds.clear();
        createdBookIds.clear();
        createdAuthorIds.clear();

        // Create a test user se non esiste
        if (!userRepository.existsById(USER_ID)) {
            RegisteredUser user = new RegisteredUser();
            user.setId(USER_ID);
            user.setUsername("testuser");
            user.setEmail("test@test.com");
            user.setReviews(new ArrayList<>());
            user.setReviewsYear(new ArrayList<>());
            userRepository.save(user);
            createdUserIds.add(USER_ID);
        }

        // Create another test user se non esiste
        if (!userRepository.existsById(OTHER_USER_ID)) {
            RegisteredUser otherUser = new RegisteredUser();
            otherUser.setId(OTHER_USER_ID);
            otherUser.setUsername("otheruser");
            otherUser.setEmail("other@test.com");
            otherUser.setReviews(new ArrayList<>());
            otherUser.setReviewsYear(new ArrayList<>());
            userRepository.save(otherUser);
            createdUserIds.add(OTHER_USER_ID);
        }

        // Create a test author se non esiste
        if (!authorRepository.existsById(AUTHOR_ID)) {
            AuthorDocument authorDoc = new AuthorDocument();
            authorDoc.setId(AUTHOR_ID);
            authorDoc.setName("Test Author");
            authorDoc.setRatingsCount(0);
            authorDoc.setSumRatings(0);
            authorRepository.save(authorDoc);
            createdAuthorIds.add(AUTHOR_ID);
        }

        // Create a test book se non esiste
        if (!bookRepository.existsById(BOOK_ID)) {
            BookDocument book = new BookDocument();
            book.setId(BOOK_ID);
            book.setTitle("Test Book");
            book.setAvailability("ACTIVE");
            book.setStatsPerYear(new ArrayList<>());
            BookDocument.Author author = new BookDocument.Author();
            author.setId(AUTHOR_ID);
            author.setName("Test Author");
            book.setAuthor(author);
            bookRepository.save(book);
            createdBookIds.add(BOOK_ID);
        }

        // Create an archived book se non esiste
        if (!bookRepository.existsById(ARCHIVED_BOOK_ID)) {
            BookDocument archivedBook = new BookDocument();
            archivedBook.setId(ARCHIVED_BOOK_ID);
            archivedBook.setTitle("Archived Book");
            archivedBook.setAvailability("ARCHIVED");
            archivedBook.setStatsPerYear(new ArrayList<>());
            BookDocument.Author author = new BookDocument.Author();
            author.setId(AUTHOR_ID);
            author.setName("Test Author");
            archivedBook.setAuthor(author);
            bookRepository.save(archivedBook);
            createdBookIds.add(ARCHIVED_BOOK_ID);
        }
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
        createdAuthorIds.forEach(id -> {
            if (authorRepository.existsById(id)) {
                authorRepository.deleteById(id);
            }
        });
    }

    private void setupUserContext(String userId, String username) {
        UserPrincipal principal = new UserPrincipal(userId, username, "USER", "active");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    private void waitForAsync() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Create Review: Basic Success")
    void testCreateReviewSuccess() {
        ReviewDTO dto = new ReviewDTO();
        dto.setBookId(BOOK_ID);
        dto.setRating(80);
        dto.setText("Great book!");

        ReviewDTO saved = reviewService.createReview(dto);
        createdReviewIds.add(saved.getId());

        assertNotNull(saved.getId());
        assertEquals(80, saved.getRating());

        waitForAsync();

        // Verify statistics
        BookDocument book = bookRepository.findById(BOOK_ID).get();
        assertNotNull(book.getStatsPerYear());
        assertFalse(book.getStatsPerYear().isEmpty());
        assertEquals(1, book.getStatsPerYear().get(0).getRatingsCount());
        assertEquals(80, book.getStatsPerYear().get(0).getSumRating());

        // Verify month score (since it's current month)
        assertNotNull(book.getMonthScore());
        assertEquals(1, book.getMonthScore().getRatingCount());
        assertEquals(80, book.getMonthScore().getSumRating());
    }

    @Test
    @Order(2)
    @DisplayName("Create Review: Duplicate Failure")
    void testCreateReviewDuplicate() {
        ReviewDTO dto = new ReviewDTO();
        dto.setBookId(BOOK_ID);
        dto.setRating(80);
        dto.setText("First review");

        ReviewDTO created = reviewService.createReview(dto);
        createdReviewIds.add(created.getId());

        // Try to create another review for the same book
        ReviewDTO dto2 = new ReviewDTO();
        dto2.setBookId(BOOK_ID);
        dto2.setRating(90);
        dto2.setText("Second review");

        assertThrows(AlreadyExistsException.class, () -> reviewService.createReview(dto2));
    }

    @Test
    @Order(3)
    @DisplayName("Create Review: Archived Book Failure")
    void testCreateReviewArchivedBook() {
        ReviewDTO dto = new ReviewDTO();
        dto.setBookId(ARCHIVED_BOOK_ID);
        dto.setRating(80);
        dto.setText("Attempt on archived book");

        assertThrows(BookArchivedException.class, () -> reviewService.createReview(dto));
    }

    @Test
    @Order(4)
    @DisplayName("Update Review: Unauthorized user")
    void testUpdateReviewUnauthorized() {
        // Create review as user 1
        ReviewDTO dto = new ReviewDTO();
        dto.setBookId(BOOK_ID);
        dto.setRating(80);
        ReviewDTO created = reviewService.createReview(dto);
        createdReviewIds.add(created.getId());
        String reviewId = created.getId();

        // Change context to user 2
        setupUserContext(OTHER_USER_ID, "otheruser");

        ReviewDTO updateDto = new ReviewDTO();
        updateDto.setRating(100);

        assertThrows(UnauthorizedOperationException.class, () -> reviewService.updateReview(reviewId, updateDto));
    }

    @Test
    @Order(5)
    @DisplayName("Update Review: Archived Book (Rating Change)")
    void testUpdateReviewArchivedBook() {
        // 1. Manually create a review for the archived book in DB
        Review review = new Review();
        review.setUserId(USER_ID);
        review.setUsername("testuser");
        review.setRating(50);
        review.setBookSnapshot(new Review.BookSnapshot(ARCHIVED_BOOK_ID, "Archived Book"));
        review.setCreatedAt(Instant.now());
        review = reviewRepository.save(review);
        createdReviewIds.add(review.getId());

        // Ensure Neo4j node exists to avoid swallowing other logic in updateReview
        setupNeo4jReview(review.getId(), 50);

        // Add to user reviews
        RegisteredUser user = userRepository.findById(USER_ID).get();
        user.getReviews().add(review.getId());
        userRepository.save(user);

        // 2. Try to update rating of review for archived book
        ReviewDTO updateDto = new ReviewDTO();
        updateDto.setRating(100);
        String finalReviewId = review.getId();
        assertThrows(BookArchivedException.class, () -> reviewService.updateReview(finalReviewId, updateDto));
    }

    @Test
    @Order(5)
    @DisplayName("Delete Review: Not Owner Not Admin")
    void testDeleteReviewUnauthorized() {
        // Create review as user 1
        Review review = new Review();
        review.setUserId(USER_ID);
        review.setBookSnapshot(new Review.BookSnapshot(BOOK_ID, "Test Book"));
        review = reviewRepository.save(review);
        createdReviewIds.add(review.getId());
        String reviewId = review.getId();

        // Change context to user 2
        setupUserContext(OTHER_USER_ID, "otheruser");

        assertThrows(UnauthorizedOperationException.class, () -> reviewService.deleteReview(reviewId));
    }

    @Test
    @Order(7)
    @DisplayName("Strange Case: Update Review from Previous Year")
    void testUpdateOldReviewStats() {
        // 1. Create a review from 2024
        LocalDateTime lastYear = LocalDateTime.of(2024, 1, 1, 12, 0);
        Instant lastYearInstant = lastYear.toInstant(ZoneOffset.UTC);

        Review review = new Review();
        review.setUserId(USER_ID);
        review.setUsername("testuser");
        review.setRating(50);
        review.setCreatedAt(lastYearInstant);
        review.setBookSnapshot(new Review.BookSnapshot(BOOK_ID, "Test Book"));
        review = reviewRepository.save(review);
        createdReviewIds.add(review.getId());

        // Setup Neo4j node
        setupNeo4jReview(review.getId(), 50);

        // Add to user reviews
        RegisteredUser user = userRepository.findById(USER_ID).get();
        user.getReviews().add(review.getId());
        userRepository.save(user);

        // Manually setup book stats for 2024
        BookDocument book = bookRepository.findById(BOOK_ID).get();
        List<BookDocument.YearStat> stats = new ArrayList<>();
        stats.add(new BookDocument.YearStat(2024, 1, 50));
        book.setStatsPerYear(stats);
        
        // Also set a month score for current month to verify it's NOT changed
        String currentMonth = YearMonth.now().toString();
        book.setMonthScore(new BookDocument.MonthScore(1, 100, currentMonth));
        bookRepository.save(book);

        // 2. Update the review rating
        ReviewDTO updateDto = new ReviewDTO();
        updateDto.setRating(100);
        reviewService.updateReview(review.getId(), updateDto);

        waitForAsync();

        // 3. Verify
        BookDocument updatedBook = bookRepository.findById(BOOK_ID).get();
        
        // Verify stats_per_year for 2024 was updated
        BookDocument.YearStat updatedYearStat = updatedBook.getStatsPerYear().stream()
                .filter(s -> s.getYear().equals(2024))
                .findFirst().orElseThrow();
        assertEquals(1, updatedYearStat.getRatingsCount());
        assertEquals(100, updatedYearStat.getSumRating());

        // Verify current month score was NOT touched
        assertEquals(1, updatedBook.getMonthScore().getRatingCount());
        assertEquals(100, updatedBook.getMonthScore().getSumRating());
    }

    @Test
    @Order(8)
    @DisplayName("Strange Case: Delete the only Review of a Year")
    void testDeleteOnlyReviewOfYear() {
        // 1. Create a review from 2023
        LocalDateTime oldYear = LocalDateTime.of(2023, 1, 1, 12, 0);
        Instant oldYearInstant = oldYear.toInstant(ZoneOffset.UTC);

        Review review = new Review();
        review.setUserId(USER_ID);
        review.setRating(50);
        review.setCreatedAt(oldYearInstant);
        review.setBookSnapshot(new Review.BookSnapshot(BOOK_ID, "Test Book"));
        review = reviewRepository.save(review);
        createdReviewIds.add(review.getId());

        // Manually setup book stats for 2023
        BookDocument book = bookRepository.findById(BOOK_ID).get();
        List<BookDocument.YearStat> stats = new ArrayList<>();
        stats.add(new BookDocument.YearStat(2023, 1, 50));
        book.setStatsPerYear(stats);
        bookRepository.save(book);

        // 2. Delete the review
        reviewService.deleteReview(review.getId());

        waitForAsync();

        // 3. Verify
        BookDocument updatedBook = bookRepository.findById(BOOK_ID).get();

        System.out.println(updatedBook.getStatsPerYear());

        // Verify 2023 entry is removed
        assertTrue(updatedBook.getStatsPerYear().stream().noneMatch(s -> s.getYear().equals(2023)));
    }

    @Test
    @Order(9)
    @DisplayName("Update Review: Text only (No Stats Update)")
    void testUpdateReviewTextOnly() {
        // 1. Create review
        ReviewDTO dto = new ReviewDTO();
        dto.setBookId(BOOK_ID);
        dto.setRating(80);
        dto.setText("Initial text");
        ReviewDTO saved = reviewService.createReview(dto);
        createdReviewIds.add(saved.getId());
        
        waitForAsync();

        // 2. Update only text
        ReviewDTO updateDto = new ReviewDTO();
        updateDto.setText("Updated text only");
        reviewService.updateReview(saved.getId(), updateDto);
        
        waitForAsync();

        // 3. Verify stats haven't changed (ratings_count was 1, sum_rating was 80)
        BookDocument book = bookRepository.findById(BOOK_ID).get();
        assertEquals(1, book.getStatsPerYear().get(0).getRatingsCount());
        assertEquals(80, book.getStatsPerYear().get(0).getSumRating());
    }

    @Test
    @Order(10)
    @DisplayName("Get Reviews by IDs: Mixed validity")
    void testGetReviewsByIds() {
        // Create one review
        Review review = new Review();
        review.setUserId(USER_ID);
        review = reviewRepository.save(review);
        createdReviewIds.add(review.getId());
        
        List<String> ids = List.of(review.getId(), "nonexistent123456789012");
        List<ReviewDTO> found = reviewService.getReviewsByIds(ids);
        
        assertEquals(1, found.size());
        assertEquals(review.getId(), found.get(0).getId());
    }
}
