package it.unipi.bookSphere.repository.neo4j;

import it.unipi.bookSphere.model.neo4j.BookNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing BookNode entities in Neo4j
 */
@Repository
public interface BookNodeRepository extends Neo4jRepository<BookNode, String> {
    
    /**
     * Find book by MongoDB ID
     */
    Optional<BookNode> findByMongoId(String mongoId);
    
    /**
     * Delete book by MongoDB ID
     */
    @Query("MATCH (b:Book {mongoId: $mongoId}) DETACH DELETE b")
    void deleteByMongoId(@Param("mongoId") String mongoId);
    
    /**
     * Check if book exists by MongoDB ID
     */
    boolean existsByMongoId(String mongoId);
    
    // ========== LIKES RELATIONSHIP METHODS ==========
    
    /**
     * Check if user likes a book
     */
    @Query("MATCH (u:User {mongoId: $userId})-[r:LIKES]->(b:Book {mongoId: $bookId}) RETURN COUNT(r) > 0")
    boolean userLikesBook(@Param("userId") String userId, @Param("bookId") String bookId);
    
    /**
     * Create LIKES relationship between user and book
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})
        MATCH (b:Book {mongoId: $bookId})
        CREATE (u)-[r:LIKES {timestamp: $timestamp}]->(b)
        RETURN r
        """)
    void createLikesRelationship(
        @Param("userId") String userId,
        @Param("bookId") String bookId,
        @Param("timestamp") LocalDateTime timestamp
    );
    
    /**
     * Delete LIKES relationship between user and book
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[r:LIKES]->(b:Book {mongoId: $bookId})
        DELETE r
        RETURN COUNT(r)
        """)
    Long deleteLikesRelationship(
        @Param("userId") String userId,
        @Param("bookId") String bookId
    );
    
    /**
     * Get all books liked by a user
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[r:LIKES]->(b:Book)
        RETURN b.mongoId AS bookId, b.title AS title, r.timestamp AS timestamp
        ORDER BY r.timestamp DESC
        """)
    List<Map<String, Object>> getLikedBooksByUser(@Param("userId") String userId);
    
    // ========== BELONGS_TO RELATIONSHIP METHODS ==========
    
    /**
     * Create BELONGS_TO relationship between book and genre
     */
    @Query("""
        MATCH (b:Book {mongoId: $bookMongoId})
        MATCH (g:Genre {name: $genreName})
        MERGE (b)-[r:BELONGS_TO]->(g)
        RETURN r
        """)
    void createBelongsToRelationship(
        @Param("bookMongoId") String bookMongoId,
        @Param("genreName") String genreName
    );
    
    /**
     * Delete all BELONGS_TO relationships for a specific book
     */
    @Query("""
        MATCH (b:Book {mongoId: $bookMongoId})-[r:BELONGS_TO]->(g:Genre)
        DELETE r
        RETURN COUNT(r)
        """)
    Long deleteBelongsToRelationshipsForBook(@Param("bookMongoId") String bookMongoId);
    
    // ========== GET OR CREATE METHODS ==========
    
    /**
     * Get or create BookNode - if not found, creates a new one with the given parameters
     * This is a default method to centralize the get-or-create pattern used across services
     */
    default BookNode getOrCreate(String mongoId, String title, Integer year) {
        return findByMongoId(mongoId)
                .orElseGet(() -> {
                    BookNode newNode = new BookNode();
                    newNode.setMongoId(mongoId);
                    newNode.setTitle(title);
                    newNode.setYear(year);
                    return save(newNode);
                });
    }
}
