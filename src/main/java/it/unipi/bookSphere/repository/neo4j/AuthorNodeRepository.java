package it.unipi.bookSphere.repository.neo4j;

import it.unipi.bookSphere.model.neo4j.AuthorNode;
import it.unipi.bookSphere.repository.neo4j.projections.AuthorLikeProjection;
import it.unipi.bookSphere.repository.neo4j.projections.InternationalityProjection;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing AuthorNode entities in Neo4j
 */
@Repository
public interface AuthorNodeRepository extends Neo4jRepository<AuthorNode, String> {
    
    /**
     * Find author by MongoDB ID
     */
    Optional<AuthorNode> findByMongoId(String mongoId);
    
    /**
     * Delete author by MongoDB ID
     */
    @Query("MATCH (a:Author {mongoId: $mongoId}) DETACH DELETE a")
    void deleteByMongoId(@Param("mongoId") String mongoId);
    
    /**
     * Check if author exists by MongoDB ID
     */
    boolean existsByMongoId(String mongoId);
    
    // ========== LIKES RELATIONSHIP METHODS ==========
    
    /**
     * Check if user likes an author
     */
    @Query("MATCH (u:User {mongoId: $userId})-[r:LIKES]->(a:Author {mongoId: $authorId}) RETURN COUNT(r) > 0")
    boolean userLikesAuthor(@Param("userId") String userId, @Param("authorId") String authorId);
    
    /**
     * Create LIKES relationship between user and author
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})
        MATCH (a:Author {mongoId: $authorId})
        CREATE (u)-[r:LIKES {timestamp: $timestamp}]->(a)
        RETURN r
        """)
    void createLikesRelationship(
        @Param("userId") String userId,
        @Param("authorId") String authorId,
        @Param("timestamp") LocalDateTime timestamp
    );
    
    /**
     * Delete LIKES relationship between user and author
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[r:LIKES]->(a:Author {mongoId: $authorId})
        DELETE r
        RETURN COUNT(r)
        """)
    Long deleteLikesRelationship(
        @Param("userId") String userId,
        @Param("authorId") String authorId
    );
    
    /**
     * Get all authors liked by a user
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[r:LIKES]->(a:Author)
        RETURN a.mongoId AS authorId, a.name AS name, r.timestamp AS timestamp
        ORDER BY r.timestamp DESC
        """)
    List<AuthorLikeProjection> getLikedAuthorsByUser(@Param("userId") String userId);
    
    /**
     * Get all authors liked by a user with pagination
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[r:LIKES]->(a:Author)
        RETURN a.mongoId AS authorId, a.name AS name, r.timestamp AS timestamp
        ORDER BY r.timestamp DESC
        SKIP $skip LIMIT $limit
        """)
    List<AuthorLikeProjection> getLikedAuthorsByUser(
        @Param("userId") String userId, 
        @Param("skip") long skip, 
        @Param("limit") int limit
    );
    
    /**
     * Count liked authors for pagination
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[:LIKES]->(a:Author)
        RETURN count(a)
        """)
    long countLikedAuthorsByUser(@Param("userId") String userId);
    
    // ========== WROTE RELATIONSHIP METHODS ==========
    
    /**
     * Create WROTE relationship between author and book
     */
    @Query("""
        MATCH (a:Author {mongoId: $authorMongoId})
        MATCH (b:Book {mongoId: $bookMongoId})
        CREATE (a)-[r:WROTE]->(b)
        RETURN r
        """)
    void createWroteRelationship(
        @Param("authorMongoId") String authorMongoId,
        @Param("bookMongoId") String bookMongoId
    );
    
    /**
     * Delete all WROTE relationships for a specific book
     */
    @Query("""
        MATCH (a:Author)-[r:WROTE]->(b:Book {mongoId: $bookMongoId})
        DELETE r
        RETURN COUNT(r)
        """)
    Long deleteWroteRelationshipsForBook(@Param("bookMongoId") String bookMongoId);
    
    // ========== GET OR CREATE METHODS ==========
    
    /**
     * Get or create AuthorNode - if not found, creates a new one with the given parameters
     * This is a default method to centralize the get-or-create pattern used across services
     */
    default AuthorNode getOrCreate(String mongoId, String name) {
        return findByMongoId(mongoId)
                .orElseGet(() -> {
                    AuthorNode newNode = new AuthorNode();
                    newNode.setMongoId(mongoId);
                    newNode.setName(name);
                    return save(newNode);
                });
    }
    
    // ========== ANALYTICS METHODS ==========
    
    /**
     * Calculate Internationality Index for an author
     * Returns the geographical distribution of likes and reviews for the author
     */
    @Query("""
        MATCH (a:Author {mongoId: $authorId})
        OPTIONAL MATCH (a)<-[:LIKES]-(liker:User)
        OPTIONAL MATCH (a)-[:WROTE]->(b:Book)<-[:REFER_TO]-(r:Review)<-[:POSTED]-(reviewer:User)
        WITH liker, reviewer
        WITH collect(DISTINCT liker) + collect(DISTINCT reviewer) AS users
        UNWIND users AS u
        WITH u WHERE u IS NOT NULL AND u.country IS NOT NULL
        RETURN u.country AS country, count(DISTINCT u.mongoId) AS uniqueUsers, count(*) AS totalInteractions
        ORDER BY uniqueUsers DESC
        """)
    List<InternationalityProjection> calculateAuthorInternationality(@Param("authorId") String authorId);
}
