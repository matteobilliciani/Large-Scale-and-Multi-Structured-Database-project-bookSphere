package it.unipi.bookSphere.repository.neo4j;

import it.unipi.bookSphere.model.neo4j.ReviewNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing ReviewNode entities in Neo4j
 */
@Repository
public interface ReviewNodeRepository extends Neo4jRepository<ReviewNode, String> {
    
    /**
     * Find review by MongoDB ID
     */
    Optional<ReviewNode> findByMongoId(String mongoId);
    
    /**
     * Delete review by MongoDB ID
     */
    @Query("MATCH (r:Review {mongoId: $mongoId}) DETACH DELETE r")
    void deleteByMongoId(@Param("mongoId") String mongoId);
    
    /**
     * Check if review exists by MongoDB ID
     */
    boolean existsByMongoId(String mongoId);
    
    // ========== LIKES RELATIONSHIP METHODS ==========
    
    /**
     * Check if user likes a review
     */
    @Query("MATCH (u:User {mongoId: $userId})-[r:LIKES]->(rev:Review {mongoId: $reviewId}) RETURN COUNT(r) > 0")
    boolean userLikesReview(@Param("userId") String userId, @Param("reviewId") String reviewId);
    
    /**
     * Create LIKES relationship between user and review
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})
        MATCH (r:Review {mongoId: $reviewId})
        CREATE (u)-[l:LIKES {timestamp: $timestamp}]->(r)
        RETURN l
        """)
    void createLikesRelationship(
        @Param("userId") String userId,
        @Param("reviewId") String reviewId,
        @Param("timestamp") LocalDateTime timestamp
    );
    
    /**
     * Delete LIKES relationship between user and review
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[r:LIKES]->(rev:Review {mongoId: $reviewId})
        DELETE r
        RETURN COUNT(r)
        """)
    Long deleteLikesRelationship(
        @Param("userId") String userId,
        @Param("reviewId") String reviewId
    );
    
    /**
     * Get all reviews liked by a user
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[l:LIKES]->(r:Review)
        RETURN r.mongoId AS reviewId, r.rating AS rating, l.timestamp AS timestamp
        ORDER BY l.timestamp DESC
        """)
    List<Map<String, Object>> getLikedReviewsByUser(@Param("userId") String userId);
    
    /**
     * Get all reviews liked by a user with pagination
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[l:LIKES]->(r:Review)
        RETURN r.mongoId AS reviewId, r.rating AS rating, l.timestamp AS timestamp
        ORDER BY l.timestamp DESC
        SKIP $skip LIMIT $limit
        """)
    List<Map<String, Object>> getLikedReviewsByUser(
        @Param("userId") String userId, 
        @Param("skip") long skip, 
        @Param("limit") int limit
    );
    
    /**
     * Count liked reviews for pagination
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[:LIKES]->(r:Review)
        RETURN count(r)
        """)
    long countLikedReviewsByUser(@Param("userId") String userId);
    
    // ========== REVIEW RELATIONSHIP METHODS ==========
    
    /**
     * Create POSTED relationship between user and review
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})
        MATCH (r:Review {mongoId: $reviewId})
        MERGE (u)-[:POSTED]->(r)
        """)
    void createPostedRelationship(
        @Param("userId") String userId,
        @Param("reviewId") String reviewId
    );
    
    /**
     * Create REFER_TO relationship between review and book
     */
    @Query("""
        MATCH (r:Review {mongoId: $reviewId})
        MATCH (b:Book {mongoId: $bookId})
        MERGE (r)-[:REFER_TO]->(b)
        """)
    void createReferToRelationship(
        @Param("reviewId") String reviewId,
        @Param("bookId") String bookId
    );
}
