package it.unipi.bookSphere.open;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.service.open.AuthorService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import it.unipi.bookSphere.TestProfile;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for Author APIs (OPEN - No authentication required)
 * Tests:
 * - Get author by ID (GET /api/v1/authors/{id})
 * - Search authors by name (GET /api/v1/authors?author_name=...)
 * - Pagination
 * - Error handling for non-existent authors
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthorControllerTest {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private AuthorRepository authorRepository;

    private static final String TEST_PREFIX = "AuthorTest ";
    private static String testAuthorId1;
    private static String testAuthorId2;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");

        authorRepository.findAll().stream()
                .filter(a -> a.getName() != null && a.getName().startsWith(TEST_PREFIX))
                .forEach(author -> {
                    authorRepository.deleteById(author.getId());
                    System.out.println("Deleted test author: " + author.getName());
                });

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Setup - Create test data")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Test Data ===");

        // Create test author 1
        AuthorDocument author1 = new AuthorDocument();
        author1.setName(TEST_PREFIX + "J.R.R. Tolkien");
        author1.setRatingsCount(0);
        author1.setSumRatings(0);
        author1.setStatus("ACTIVE");
        author1 = authorRepository.save(author1);
        testAuthorId1 = author1.getId();
        System.out.println("Created test author 1: " + author1.getName());

        // Create test author 2
        AuthorDocument author2 = new AuthorDocument();
        author2.setName(TEST_PREFIX + "George R.R. Martin");
        author2.setRatingsCount(0);
        author2.setSumRatings(0);
        author2.setStatus("ACTIVE");
        author2 = authorRepository.save(author2);
        testAuthorId2 = author2.getId();
        System.out.println("Created test author 2: " + author2.getName());
    }

    @Test
    @Order(3)
    @DisplayName("03. Get author by ID - Success")
    void test03_GetAuthorById_Success() {
        System.out.println("\n=== TEST 03: Get Author by ID ===");

        AuthorDTO author = authorService.findById(testAuthorId1);

        assertNotNull(author);
        assertEquals(testAuthorId1, author.getId());
        assertEquals(TEST_PREFIX + "J.R.R. Tolkien", author.getName());

        System.out.println("Author retrieved: " + author.getName());
    }

    @Test
    @Order(4)
    @DisplayName("04. Get author by ID - Not found")
    void test04_GetAuthorById_NotFound() {
        System.out.println("\n=== TEST 04: Get Non-existent Author ===");

        String fakeId = "000000000000000000000000";

        Exception exception = assertThrows(Exception.class, () -> {
            authorService.findById(fakeId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("05. Search authors by name - Full match")
    void test05_SearchByName_FullMatch() {
        System.out.println("\n=== TEST 05: Search Authors by Name (Full Match) ===");

        // Search for "Tolkien" which should match our test author
        Page<AuthorDTO> authors = authorService.searchByName("Tolkien", 0, 20);

        assertNotNull(authors);
        assertTrue(authors.getTotalElements() >= 1, "Expected to find at least 1 author with 'Tolkien' in name");
        assertTrue(authors.getContent().stream()
                .anyMatch(a -> a.getName().contains("Tolkien")), 
                "Expected at least one author to have 'Tolkien' in name");

        System.out.println("Found " + authors.getTotalElements() + " author(s)");
    }

    @Test
    @Order(6)
    @DisplayName("06. Search authors by name - Partial match")
    void test06_SearchByName_PartialMatch() {
        System.out.println("\n=== TEST 06: Search Authors by Name (Partial Match) ===");

        // Text search works with complete words, so we search for "AuthorTest" (without underscore)
        Page<AuthorDTO> authors = authorService.searchByName("AuthorTest", 0, 20);

        assertNotNull(authors);
        assertTrue(authors.getTotalElements() >= 2);
        System.out.println("Found " + authors.getTotalElements() + " author(s) with text search");
    }

    @Test
    @Order(7)
    @DisplayName("07. Search authors - No results")
    void test07_SearchByName_NoResults() {
        System.out.println("\n=== TEST 07: Search Authors (No Results) ===");

        Page<AuthorDTO> authors = authorService.searchByName("NonExistentAuthor123456", 0, 20);

        assertNotNull(authors);
        assertEquals(0, authors.getTotalElements());
        System.out.println("No authors found as expected");
    }

    @Test
    @Order(8)
    @DisplayName("08. Search authors - Pagination")
    void test08_SearchByName_Pagination() {
        System.out.println("\n=== TEST 08: Search Authors with Pagination ===");

        // Page 0, size 1 - Text search works with complete words
        Page<AuthorDTO> page1 = authorService.searchByName("AuthorTest", 0, 1);
        assertEquals(1, page1.getContent().size());
        assertTrue(page1.getTotalElements() >= 2);

        // Page 1, size 1
        Page<AuthorDTO> page2 = authorService.searchByName("AuthorTest", 1, 1);
        assertEquals(1, page2.getContent().size());

        // Verify different authors
        assertNotEquals(page1.getContent().get(0).getId(), page2.getContent().get(0).getId());

        System.out.println("Pagination working correctly");
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        if (testAuthorId1 != null) {
            authorRepository.deleteById(testAuthorId1);
            System.out.println("Deleted test author 1");
        }
        if (testAuthorId2 != null) {
            authorRepository.deleteById(testAuthorId2);
            System.out.println("Deleted test author 2");
        }

        System.out.println("Final cleanup completed - All test data removed");
    }
}
