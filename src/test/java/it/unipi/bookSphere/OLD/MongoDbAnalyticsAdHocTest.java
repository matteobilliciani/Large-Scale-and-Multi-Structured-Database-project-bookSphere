package it.unipi.bookSphere.OLD;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.model.mongodb.*;
import it.unipi.bookSphere.repository.mongo.*;
import it.unipi.bookSphere.repository.neo4j.*;
import it.unipi.bookSphere.service.*;
import it.unipi.bookSphere.utils.UserPrincipal;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MongoDbAnalyticsAdHocTest {

    // SERVICES
    @Autowired private AnalyticsService analyticsService;
    @Autowired private UserFeaturesService userFeaturesService;
    @Autowired private AuthService authService;

    // MONGO REPOS
    @Autowired private BookRepository bookRepository;
    @Autowired private AuthorRepository authorRepository;
    @Autowired private RegisteredUserRepository userRepository;

    // NEO4J REPOS (Per consistenza)
    @Autowired private AuthorNodeRepository authorNodeRepository;
    @Autowired private BookNodeRepository bookNodeRepository;
    @Autowired private GenreNodeRepository genreNodeRepository;

    // DATA CONSTANTS (FANTASY)
    private static final String USER_USERNAME = "Torch724";
    private static final String USER_PASSWORD = "password";
    private static final String USER_EMAIL = "torch724@booksphere.com";

    private static final String AUTHOR_TOP = "Arthur Pendragon";
    private static final String AUTHOR_FLOP = "Joe Nobody";

    private static final String BOOK_SLEEPER = "The Forgotten Scroll"; // Trend
    private static final String BOOK_FLOP = "The Boring Stone";
    private static final String BOOK_TOP = "The Holy Grail";           // Rank
    private static final String BOOK_MID = "Camelot";
    private static final String BOOK_EXTRA = "Extra Adventure 2026";

    private static final String GENRE_ADVENTURE = "Adventure";
    private static final String GENRE_FANTASY = "Fantasy";
    private static final String GENRE_HISTORY = "History";

    // IDs
    private static String targetUserId;
    private static String pendragonId;
    private static String nobodyId;

    @BeforeAll
    static void initInfo() {
        System.out.println("\n################################################");
        System.out.println("   MONGO ANALYTICS TEST (JAVA POPULATION)");
        System.out.println("   User: " + USER_USERNAME);
        System.out.println("################################################\n");
    }

    @BeforeEach
    void setupContext() {
        if (targetUserId != null) {
            UserPrincipal principal = new UserPrincipal(targetUserId, USER_USERNAME, "USER", "active");
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
            );
        }
    }

    // =========================================================================================
    // STEP 1: POPOLAMENTO DATI (Idempotente)
    // =========================================================================================
    @Test
    @Order(1)
    void setupData() {
        System.out.println("--- STEP 1: Setup / Load Data ---");

        // 1. UTENTE (AuthService)
        setupUser();

        // 2. AUTORI & GENERI
        setupAuthorsAndGenres();

        // 3. LIBRI (Con Statistiche)
        // Arthur Pendragon (Top Ratings)
        setupBook(BOOK_TOP, pendragonId, AUTHOR_TOP, GENRE_ADVENTURE,
                2020, 850, 10,
                2026, 2850, 30); // Avg 95.0

        setupBook(BOOK_MID, pendragonId, AUTHOR_TOP, GENRE_ADVENTURE,
                2015, 900, 10,
                2026, 3680, 40); // Avg 92.0

        setupBook(BOOK_EXTRA, pendragonId, AUTHOR_TOP, GENRE_ADVENTURE,
                2026, 90, 1,
                2026, 90, 1);

        // Joe Nobody (Trend)
        // Sleeper Hit: 2018 (0.1) -> 2026 (100.0) -> Delta 99.9
        setupBook(BOOK_SLEEPER, nobodyId, AUTHOR_FLOP, GENRE_FANTASY,
                2018, 1, 10,
                2026, 2000, 20);

        setupBook(BOOK_FLOP, nobodyId, AUTHOR_FLOP, GENRE_HISTORY,
                2018, 900, 10,
                2026, 800, 20);

        // 4. ATTIVITÀ UTENTE (Per Wrapped)
        setupUserActivity();

        System.out.println("✓ Data Setup Completed.");
    }

    private void setupUser() {
        Optional<RegisteredUser> existing = userRepository.findByUsername(USER_USERNAME);
        if (existing.isPresent()) {
            targetUserId = existing.get().getId();
        } else {
            RegisterDTO reg = new RegisterDTO();
            reg.setUsername(USER_USERNAME);
            reg.setEmail(USER_EMAIL);
            reg.setPassword(USER_PASSWORD);
            reg.setCountry("IT");
            UserDTO u = authService.register(reg);
            targetUserId = u.getId();
        }
    }

    private void setupAuthorsAndGenres() {
        // Genres
        for (String g : List.of(GENRE_ADVENTURE, GENRE_FANTASY, GENRE_HISTORY)) {
            if (!genreNodeRepository.existsByName(g)) {
                genreNodeRepository.save(new it.unipi.bookSphere.model.neo4j.GenreNode(g));
            }
        }
        // Authors
        pendragonId = createOrGetAuthor(AUTHOR_TOP);
        nobodyId = createOrGetAuthor(AUTHOR_FLOP);
    }

    private String createOrGetAuthor(String name) {
        return authorRepository.findByName(name)
                .map(AuthorDocument::getId)
                .orElseGet(() -> {
                    AuthorDocument a = new AuthorDocument();
                    a.setName(name);
                    a.setStatus("ACTIVE");
                    a.setPublishedBooks(new ArrayList<>());
                    a = authorRepository.save(a);
                    // Sync Neo4j
                    authorNodeRepository.save(new it.unipi.bookSphere.model.neo4j.AuthorNode(a.getId(), name));
                    return a.getId();
                });
    }

    private void setupBook(String title, String authId, String authName, String genre,
                           int y1, int sum1, int count1, int y2, int sum2, int count2) {

        if (bookRepository.findByTitle(title).isPresent()) return;

        BookDocument book = new BookDocument();
        book.setTitle(title);
        book.setAuthor(new BookDocument.Author(authId, authName));
        book.setGenres(List.of(genre));
        book.setAvailability("ACTIVE");
        book.setPublicationYear(y1);

        // Stats
        List<BookDocument.YearStat> stats = new ArrayList<>();
        stats.add(createStat(y1, sum1, count1));
        if (y1 != y2) stats.add(createStat(y2, sum2, count2));
        book.setStatsPerYear(stats);

        // Month Score (Gennaio 2026)
        BookDocument.MonthScore ms = new BookDocument.MonthScore();
        ms.setCurrentMonth("2026-01");
        ms.setSumRating(sum2 / 2);
        ms.setRatingCount(count2 / 2);
        book.setMonthScore(ms);

        book = bookRepository.save(book);

        // Update Author (Published Books)
        AuthorDocument author = authorRepository.findById(authId).orElseThrow();
        AuthorDocument.PublishedBook pb = new AuthorDocument.PublishedBook();
        pb.setId(book.getId());
        pb.setTitle(title);
        author.getPublishedBooks().add(pb);
        authorRepository.save(author);

        // Neo4j Node
        bookNodeRepository.save(new it.unipi.bookSphere.model.neo4j.BookNode(book.getId(), title, y1));
    }

    private BookDocument.YearStat createStat(int year, int sum, int count) {
        BookDocument.YearStat s = new BookDocument.YearStat();
        s.setYear(year); s.setSumRating(sum); s.setRatingsCount(count);
        return s;
    }

    private void setupUserActivity() {
        RegisteredUser user = userRepository.findById(targetUserId).orElseThrow();
        if (user.getBookshelf() != null && !user.getBookshelf().isEmpty()) return;

        List<RegisteredUser.BookshelfItem> shelf = new ArrayList<>();
        List<RegisteredUser.ReviewYear> revs = new ArrayList<>();

        // Torch724 legge 3 libri Adventure di Pendragon nel 2026
        addActivity(shelf, revs, BOOK_TOP, AUTHOR_TOP, GENRE_ADVENTURE, 100);
        addActivity(shelf, revs, BOOK_MID, AUTHOR_TOP, GENRE_ADVENTURE, 90);
        addActivity(shelf, revs, BOOK_EXTRA, AUTHOR_TOP, GENRE_ADVENTURE, 85);

        user.setBookshelf(shelf);
        user.setReviewsYear(revs);
        userRepository.save(user);
    }

    private void addActivity(List<RegisteredUser.BookshelfItem> shelf, List<RegisteredUser.ReviewYear> revs,
                             String title, String aName, String genre, int rating) {
        bookRepository.findByTitle(title).ifPresent(b -> {
            RegisteredUser.BookshelfItem item = new RegisteredUser.BookshelfItem();
            item.setBookId(b.getId());
            item.setTitle(title);
            item.setAuthor(new RegisteredUser.Author(b.getAuthor().getId(), aName));
            item.setGenres(List.of(genre));
            item.setStatus("read");
            item.setAddedAt(Instant.parse("2026-01-15T10:00:00Z"));
            shelf.add(item);

            RegisteredUser.ReviewYear r = new RegisteredUser.ReviewYear();
            r.setId("rev_" + b.getId());
            r.setBook(title);
            r.setRating(rating);
            revs.add(r);
        });
    }

    // =========================================================================================
    // STEP 2: TEST ANALYTICS (Query Reali)
    // =========================================================================================

    @Test
    @Order(2)
    void testTrendRevaluation() {
        System.out.println("\n--- TEST 2: Trend Revaluation ---");
        List<BookTrendDTO> trends = analyticsService.getBookRevaluation();

        trends.forEach(t -> System.out.println(String.format("  #%d %s: Delta %+.2f",
                t.getPosition(), t.getTitle(), t.getRatingDelta())));

        // Verifica: "The Forgotten Scroll" deve avere Delta > 90 (0.1 -> 100)
        BookTrendDTO sleeper = trends.stream()
                .filter(b -> b.getTitle().equals(BOOK_SLEEPER))
                .findFirst()
                .orElse(null);

        assertNotNull(sleeper, "Sleeper hit not found in trends");
        assertTrue(sleeper.getRatingDelta() > 90.0, "Delta should be massive");
        System.out.println("✓ PASSED: Revaluation works");
    }

    @Test
    @Order(3)
    void testBookRankingsByAuthor_AllTime() {
        System.out.println("\n--- TEST 3: Book Rankings (Author: Arthur Pendragon, All-Time) ---");

        // Chiamata: Year=null, Author=Pendragon, Genre=null
        List<RankingDTO> result = analyticsService.getBookRankings(null, pendragonId, null);

        result.forEach(dto -> System.out.println(
                String.format("  '%s' by %s - rating=%.2f", dto.getName(), dto.getAdditionalInfo(), dto.getAverageRating())
        ));

        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Verifica 1: Solo libri di Pendragon
        boolean hasOther = result.stream().anyMatch(d -> !d.getAdditionalInfo().equals(AUTHOR_TOP));
        assertFalse(hasOther, "Should only contain books by " + AUTHOR_TOP);

        // Verifica 2: Contiene i libri creati
        boolean hasTop = result.stream().anyMatch(d -> d.getName().equals(BOOK_TOP));
        assertTrue(hasTop, "Should contain " + BOOK_TOP);

        System.out.println("✓ PASSED: Ranking by Author (All-Time) works");
    }

    @Test
    @Order(4)
    void testBookRankingsByGenre_Year2026() {
        System.out.println("\n--- TEST 4: Book Rankings (Genre: Adventure, Year: 2026) ---");

        // Chiamata: Year=2026, Author=null, Genre=Adventure
        List<RankingDTO> result = analyticsService.getBookRankings(2026, null, GENRE_ADVENTURE);

        result.forEach(dto -> System.out.println(
                String.format("  '%s' - rating=%.2f", dto.getName(), dto.getAverageRating())
        ));

        assertNotNull(result);
        assertFalse(result.isEmpty());

        // Verifica 1: Deve contenere Holy Grail (Adventure)
        RankingDTO grail = result.stream().filter(d -> d.getName().equals(BOOK_TOP)).findFirst().orElseThrow();
        assertEquals(95.0, grail.getAverageRating(), 1.0, "Average for 2026 should be correct");

        // Verifica 2: NON deve contenere Forgotten Scroll (Fantasy)
        boolean hasFantasy = result.stream().anyMatch(d -> d.getName().equals(BOOK_SLEEPER));
        assertFalse(hasFantasy, "Should not contain Fantasy books");

        System.out.println("✓ PASSED: Ranking by Genre (Year 2026) works");
    }

    @Test
    @Order(5)
    void testYearlyWrapped() {
        System.out.println("\n--- TEST 5: Yearly Wrapped (Torch724) ---");

        // Use UserFeaturesService (internally calls AnalyticsService with current user)
        WrappedDTO wrapped = userFeaturesService.getYearlyWrapped();

        System.out.println("  Year: " + wrapped.getYear());
        System.out.println("  Books Read: " + wrapped.getTotalBooksRead());

        // Print Top Authors
        if (!wrapped.getTopAuthors().isEmpty())
            System.out.println("  Top Author: " + wrapped.getTopAuthors().get(0).getName() +
                    " (" + wrapped.getTopAuthors().get(0).getCount() + ")");

        // Print Top Genres
        if (!wrapped.getTopGenres().isEmpty())
            System.out.println("  Top Genre: " + wrapped.getTopGenres().get(0).getName() +
                    " (" + wrapped.getTopGenres().get(0).getCount() + ")");

        // Print Best & Worst Books
        if (wrapped.getBestBook() != null) {
            System.out.println(String.format("  Best Book (Top Reviewed): '%s' - Rating: %d",
                    wrapped.getBestBook().getTitle(), wrapped.getBestBook().getRating()));
        }

        if (wrapped.getWorstBook() != null) {
            System.out.println(String.format("  Worst Book (Bottom Reviewed): '%s' - Rating: %d",
                    wrapped.getWorstBook().getTitle(), wrapped.getWorstBook().getRating()));
        }

        // --- VERIFICHE ---
        assertEquals(2026, wrapped.getYear());
        assertTrue(wrapped.getTotalBooksRead() >= 3, "Should have read 3 books");

        // Verify Top Author: Pendragon
        boolean hasPendragon = wrapped.getTopAuthors().stream()
                .anyMatch(a -> AUTHOR_TOP.equals(a.getName()));
        assertTrue(hasPendragon, "Top Authors should contain Arthur Pendragon");

        // Verify Top Genre: Adventure
        boolean hasAdventure = wrapped.getTopGenres().stream()
                .anyMatch(g -> GENRE_ADVENTURE.equals(g.getName()));
        assertTrue(hasAdventure, "Top Genres should contain Adventure");

        // Verify Best Book (The Holy Grail, rated 100)
        assertNotNull(wrapped.getBestBook(), "Best book should not be null");
        assertEquals(BOOK_TOP, wrapped.getBestBook().getTitle(), "Best book should be " + BOOK_TOP);
        assertEquals(100, wrapped.getBestBook().getRating(), "Best book rating should be 100");

        // Verify Worst Book (Extra Adventure 2026, rated 80, or Camelot 90)
        // Nello script Java abbiamo messo: Holy Grail=100, Camelot=90, Extra=85.
        // Quindi il peggiore è Extra (85).
        assertNotNull(wrapped.getWorstBook(), "Worst book should not be null");
        assertEquals(BOOK_EXTRA, wrapped.getWorstBook().getTitle(), "Worst book should be " + BOOK_EXTRA);
        assertEquals(85, wrapped.getWorstBook().getRating(), "Worst book rating should be 85");

        System.out.println("✓ PASSED: Yearly Wrapped works (including Best/Worst books)");
    }
}