package it.unipi.bookSphere.repository.neo4j;

import it.unipi.bookSphere.model.neo4j.UserNode;
import it.unipi.bookSphere.repository.neo4j.projections.RecommendationProjection;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing UserNode entities in Neo4j
 */
@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, String> {
    
    /**
     * Find user by MongoDB ID
     * @param mongoId MongoDB ObjectId
     * @return Optional containing user if found
     */
    Optional<UserNode> findByMongoId(String mongoId);
    
    /**
     * Find user by username
     * @param username Username
     * @return Optional containing user if found
     */
    Optional<UserNode> findByUsername(String username);
    
    /**
     * Check if user exists by MongoDB ID
     * @param mongoId MongoDB ObjectId
     * @return true if user exists
     */
    boolean existsByMongoId(String mongoId);
    
    /**
     * Delete user by MongoDB ID
     * @param mongoId MongoDB ObjectId
     */
    @Query("MATCH (u:User {mongoId: $mongoId}) DETACH DELETE u")
    void deleteByMongoId(@Param("mongoId") String mongoId);
    
    // ========== FOLLOW RELATIONSHIP METHODS ==========
    
    /**
     * Check if user A follows user B
     */
    @Query("MATCH (a:User {mongoId: $userAId})-[r:FOLLOWS]->(b:User {mongoId: $userBId}) RETURN COUNT(r) > 0")
    boolean isFollowing(@Param("userAId") String userAId, @Param("userBId") String userBId);
    
    /**
     * Create FOLLOWS relationship between two users
     */
    @Query("""
        MATCH (a:User {mongoId: $followerId})
        MATCH (b:User {mongoId: $followedId})
        CREATE (a)-[r:FOLLOWS {since: $since}]->(b)
        RETURN r
        """)
    void createFollowsRelationship(
        @Param("followerId") String followerId, 
        @Param("followedId") String followedId,
        @Param("since") LocalDateTime since
    );
    
    /**
     * Delete FOLLOWS relationship between two users
     * @return number of relationships deleted (0 or 1)
     */
    @Query("""
        MATCH (a:User {mongoId: $followerId})-[r:FOLLOWS]->(b:User {mongoId: $followedId})
        DELETE r
        RETURN COUNT(r)
        """)
    Long deleteFollowsRelationship(
        @Param("followerId") String followerId,
        @Param("followedId") String followedId
    );
    
    /**
     * Get all users followed by a user
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[r:FOLLOWS]->(followed:User)
        RETURN followed.mongoId AS userId, followed.username AS username, 
               followed.country AS country, r.since AS since
        ORDER BY r.since DESC
        """)
    List<Map<String, Object>> getFollowedUsers(@Param("userId") String userId);
    
    /**
     * Check if user A follows user B (alias for isFollowing for backward compatibility)
     */
    @Query("MATCH (a:User {mongoId: $userAId})-[r:FOLLOWS]->(b:User {mongoId: $userBId}) RETURN COUNT(r) > 0")
    boolean userFollowsUser(@Param("userAId") String userAId, @Param("userBId") String userBId);
    
    // ========== BAN METHODS ==========
    
    /**
     * Add BannedUser label to a user node
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})
        SET u:BannedUser
        RETURN u
        """)
    void addBannedLabel(@Param("userId") String userId);
    
    // ========== GET OR CREATE METHODS ==========
    
    /**
     * Get or create UserNode - if not found, creates a new one with the given parameters
     * This is a default method to centralize the get-or-create pattern used across services
     */
    default UserNode getOrCreate(String mongoId, String username, String country) {
        return findByMongoId(mongoId)
                .orElseGet(() -> {
                    UserNode newNode = new UserNode();
                    newNode.setMongoId(mongoId);
                    newNode.setUsername(username);
                    newNode.setCountry(country);
                    return save(newNode);
                });
    }
    
    // ========== ANALYTICS METHODS ==========
    
    /**
     * Generate book recommendations for a user based on their social graph and preferences
     * Considers: books liked by followed users, books by liked authors, books in liked genres
     * Excludes books the user has already reviewed
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})
        OPTIONAL MATCH (u)-[:FOLLOWS]->(:User)-[:LIKES]->(b1:Book)
        OPTIONAL MATCH (u)-[:LIKES]->(:Author)-[:WROTE]->(b2:Book)
        OPTIONAL MATCH (u)-[:LIKES]->(:Genre)<-[:BELONGS_TO]-(b3:Book)
        WITH collect(b1) + collect(b2) + collect(b3) AS recommendations, u
        UNWIND recommendations AS book
        WITH u, book
        WHERE NOT EXISTS((u)-[:POSTED]->(:Review)-[:REFER_TO]->(book)) AND book IS NOT NULL
        WITH book.mongoId AS bookId, 
             book.title AS title, 
             book.year AS publicationYear,
             count(*) AS score
        RETURN bookId, title, publicationYear, score
        ORDER BY score DESC
        LIMIT $limit
        """)
    List<RecommendationProjection> getUserRecommendations(@Param("userId") String userId, @Param("limit") int limit);
}
