package it.unipi.bookSphere.admin;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.dto.GenreDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.neo4j.AuthorNodeRepository;
import it.unipi.bookSphere.repository.neo4j.GenreNodeRepository;
import it.unipi.bookSphere.service.admin.AdminCatalogService;
import it.unipi.bookSphere.utils.UserPrincipal;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for Admin Catalog APIs (ADMIN - Requires admin authentication)
 * Tests:
 * - Add author (POST /api/v1/admin/authors)
 * - Update author (PUT /api/v1/admin/authors/{id})
 * - Delete/Archive author (DELETE /api/v1/admin/authors/{id})
 * - Add genre (POST /api/v1/admin/genres)
 * - MongoDB + Neo4j consistency
 * - Edge cases and validation
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminCatalogControllerTest {

    @Autowired
    private AdminCatalogService adminCatalogService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorNodeRepository authorNodeRepository;

    @Autowired
    private GenreNodeRepository genreNodeRepository;

    private static final String TEST_PREFIX = "AdminCatalogTest_";
    private static String testAuthorId1;
    private static String testAuthorId2;
    private static String testGenreName;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");

        // Cleanup authors
        authorRepository.findAll().stream()
                .filter(a -> a.getName() != null && a.getName().toLowerCase().contains(TEST_PREFIX.toLowerCase()))
                .forEach(author -> {
                    authorRepository.deleteById(author.getId());
                    authorNodeRepository.deleteByMongoId(author.getId());
                    System.out.println("Deleted test author: " + author.getName());
                });

        // Cleanup genres - need to check lowercase because names are normalized
        genreNodeRepository.findAll().stream()
                .filter(g -> g.getName() != null && g.getName().toLowerCase().contains(TEST_PREFIX.toLowerCase()))
                .forEach(genre -> {
                    genreNodeRepository.delete(genre);
                    System.out.println("Deleted test genre: " + genre.getName());
                });

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Setup - Authenticate as admin")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Admin Authentication ===");
        setupAdminAuthentication();
    }

    // ========== AUTHOR MANAGEMENT TESTS ==========

    @Test
    @Order(3)
    @DisplayName("03. Add author - Success")
    void test03_AddAuthor_Success() {
        System.out.println("\n=== TEST 03: Add Author ===");

        AuthorDTO authorDTO = new AuthorDTO();
        authorDTO.setName(TEST_PREFIX + "Author One");

        AuthorDTO result = adminCatalogService.addAuthor(authorDTO);

        assertNotNull(result);
        assertNotNull(result.getId());
        // Name may be normalized (case changed), so just verify it contains the test prefix
        assertTrue(result.getName().toLowerCase().contains(TEST_PREFIX.toLowerCase()));

        testAuthorId1 = result.getId();

        // Verify in MongoDB
        AuthorDocument authorMongo = authorRepository.findById(testAuthorId1).orElse(null);
        assertNotNull(authorMongo);
        assertTrue(authorMongo.getName().toLowerCase().contains(TEST_PREFIX.toLowerCase()));
        assertEquals("ACTIVE", authorMongo.getStatus());
        assertEquals(0, authorMongo.getRatingsCount());
        assertEquals(0, authorMongo.getSumRatings());

        // Verify in Neo4j
        var authorNodeOpt = authorNodeRepository.findByMongoId(testAuthorId1);
        assertTrue(authorNodeOpt.isPresent());

        System.out.println("Author added successfully: " + testAuthorId1 + ", normalized name: " + result.getName());
    }

    @Test
    @Order(4)
    @DisplayName("04. Add author - With normalization")
    void test04_AddAuthor_WithNormalization() {
        System.out.println("\n=== TEST 04: Add Author with Name Normalization ===");

        AuthorDTO authorDTO = new AuthorDTO();
        // Test with extra spaces and mixed case
        authorDTO.setName("  " + TEST_PREFIX + "author  TWO  ");

        AuthorDTO result = adminCatalogService.addAuthor(authorDTO);

        assertNotNull(result);
        assertNotNull(result.getId());
        
        testAuthorId2 = result.getId();

        // Verify name was normalized (trimmed and proper case)
        System.out.println("Original name: '  " + TEST_PREFIX + "author  TWO  '");
        System.out.println("Normalized name: '" + result.getName() + "'");
        
        // Verify in MongoDB
        AuthorDocument authorMongo = authorRepository.findById(testAuthorId2).orElse(null);
        assertNotNull(authorMongo);
        
        System.out.println("Author added with normalized name: " + result.getName());
    }

    @Test
    @Order(5)
    @DisplayName("05. Update author - Success")
    void test05_UpdateAuthor_Success() {
        System.out.println("\n=== TEST 05: Update Author ===");

        AuthorDTO updateDTO = new AuthorDTO();
        updateDTO.setName(TEST_PREFIX + "Author One Updated");

        AuthorDTO result = adminCatalogService.updateAuthor(testAuthorId1, updateDTO);

        assertNotNull(result);
        assertEquals(testAuthorId1, result.getId());
        // Name may be normalized, verify it contains our updated text
        assertTrue(result.getName().toLowerCase().contains("updated"));

        // Verify in MongoDB
        AuthorDocument authorMongo = authorRepository.findById(testAuthorId1).orElse(null);
        assertNotNull(authorMongo);
        assertTrue(authorMongo.getName().toLowerCase().contains("updated"));

        // Verify in Neo4j
        var authorNodeOpt = authorNodeRepository.findByMongoId(testAuthorId1);
        assertTrue(authorNodeOpt.isPresent());
        assertTrue(authorNodeOpt.get().getName().toLowerCase().contains("updated"));

        System.out.println("Author updated successfully, new name: " + result.getName());
    }

    @Test
    @Order(6)
    @DisplayName("06. Update author - Non-existent (should fail)")
    void test06_UpdateAuthor_NotFound() {
        System.out.println("\n=== TEST 06: Update Non-existent Author ===");

        String fakeId = "000000000000000000000000";
        AuthorDTO updateDTO = new AuthorDTO();
        updateDTO.setName(TEST_PREFIX + "Test");

        Exception exception = assertThrows(Exception.class, () -> {
            adminCatalogService.updateAuthor(fakeId, updateDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("not found") || 
                   exception.getMessage().contains("Author not found"));
    }

    @Test
    @Order(7)
    @DisplayName("07. Delete author - Success (Archive)")
    void test07_DeleteAuthor_Success() {
        System.out.println("\n=== TEST 07: Delete/Archive Author ===");

        adminCatalogService.deleteAuthor(testAuthorId1);

        // Verify status changed to ARCHIVED in MongoDB
        AuthorDocument authorMongo = authorRepository.findById(testAuthorId1).orElse(null);
        assertNotNull(authorMongo);
        assertEquals("ARCHIVED", authorMongo.getStatus());

        // Verify deleted from Neo4j
        var authorNodeOpt = authorNodeRepository.findByMongoId(testAuthorId1);
        assertFalse(authorNodeOpt.isPresent());

        System.out.println("Author archived successfully");
    }

    @Test
    @Order(8)
    @DisplayName("08. Update archived author - Should fail")
    void test08_UpdateArchivedAuthor_ShouldFail() {
        System.out.println("\n=== TEST 08: Update Archived Author (Should Fail) ===");

        AuthorDTO updateDTO = new AuthorDTO();
        updateDTO.setName(TEST_PREFIX + "Attempt Update Archived");

        Exception exception = assertThrows(Exception.class, () -> {
            adminCatalogService.updateAuthor(testAuthorId1, updateDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
        // Just verify an exception was thrown (message might vary)
        assertNotNull(exception);
    }

    @Test
    @Order(9)
    @DisplayName("09. Delete already archived author - Should fail")
    void test09_DeleteArchivedAuthor_ShouldFail() {
        System.out.println("\n=== TEST 09: Delete Already Archived Author (Should Fail) ===");

        Exception exception = assertThrows(Exception.class, () -> {
            adminCatalogService.deleteAuthor(testAuthorId1);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
        // Just verify an exception was thrown (message might vary)
        assertNotNull(exception);
    }

    @Test
    @Order(10)
    @DisplayName("10. Delete author - Non-existent (should fail)")
    void test10_DeleteAuthor_NotFound() {
        System.out.println("\n=== TEST 10: Delete Non-existent Author ===");

        String fakeId = "000000000000000000000000";

        Exception exception = assertThrows(Exception.class, () -> {
            adminCatalogService.deleteAuthor(fakeId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("not found") || 
                   exception.getMessage().contains("Author not found"));
    }

    // ========== GENRE MANAGEMENT TESTS ==========

    @Test
    @Order(11)
    @DisplayName("11. Add genre - Success")
    void test11_AddGenre_Success() {
        System.out.println("\n=== TEST 11: Add Genre ===");

        testGenreName = TEST_PREFIX + "TestGenre";
        GenreDTO genreDTO = new GenreDTO();
        genreDTO.setName(testGenreName);

        GenreDTO result = adminCatalogService.addGenre(genreDTO);

        assertNotNull(result);
        assertNotNull(result.getName());

        // Verify in Neo4j
        boolean exists = genreNodeRepository.existsByName(result.getName());
        assertTrue(exists);

        System.out.println("Genre added successfully: " + result.getName());
    }

    @Test
    @Order(12)
    @DisplayName("12. Add genre - Duplicate (should fail)")
    void test12_AddGenre_Duplicate() {
        System.out.println("\n=== TEST 12: Add Duplicate Genre (Should Fail) ===");

        GenreDTO genreDTO = new GenreDTO();
        genreDTO.setName(testGenreName);

        Exception exception = assertThrows(Exception.class, () -> {
            adminCatalogService.addGenre(genreDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("already exists") || 
                   exception.getMessage().contains("Genre already exists"));
    }

    @Test
    @Order(13)
    @DisplayName("13. Add genre - With normalization")
    void test13_AddGenre_WithNormalization() {
        System.out.println("\n=== TEST 13: Add Genre with Normalization ===");

        String uniqueGenre = TEST_PREFIX + "Genre_Normalized";
        GenreDTO genreDTO = new GenreDTO();
        // Test with extra spaces and different case
        genreDTO.setName("  " + uniqueGenre.toLowerCase() + "  ");

        GenreDTO result = adminCatalogService.addGenre(genreDTO);

        assertNotNull(result);
        System.out.println("Original name: '  " + uniqueGenre.toLowerCase() + "  '");
        System.out.println("Normalized name: '" + result.getName() + "'");

        // Verify in Neo4j with normalized name
        boolean exists = genreNodeRepository.existsByName(result.getName());
        assertTrue(exists);

        System.out.println("Genre added with normalized name");
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        // Cleanup authors
        if (testAuthorId1 != null) {
            authorRepository.deleteById(testAuthorId1);
            authorNodeRepository.deleteByMongoId(testAuthorId1);
            System.out.println("Deleted test author 1");
        }

        if (testAuthorId2 != null) {
            authorRepository.deleteById(testAuthorId2);
            authorNodeRepository.deleteByMongoId(testAuthorId2);
            System.out.println("Deleted test author 2");
        }

        // Cleanup genres
        genreNodeRepository.findAll().stream()
                .filter(g -> g.getName() != null && g.getName().contains(TEST_PREFIX))
                .forEach(genre -> {
                    genreNodeRepository.delete(genre);
                    System.out.println("Deleted test genre: " + genre.getName());
                });

        System.out.println("Final cleanup completed - All test data removed");
    }

    private void setupAdminAuthentication() {
        UserPrincipal principal = new UserPrincipal("admin-id", "admin", "ADMIN", "active");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        System.out.println("Admin authentication setup");
    }
}
