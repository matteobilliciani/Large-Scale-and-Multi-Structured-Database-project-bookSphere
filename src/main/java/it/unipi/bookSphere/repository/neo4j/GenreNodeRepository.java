package it.unipi.bookSphere.repository.neo4j;

import it.unipi.bookSphere.model.neo4j.GenreNode;
import it.unipi.bookSphere.repository.neo4j.projections.InfluencerProjection;
import it.unipi.bookSphere.utils.NormalizationUtils;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing GenreNode entities in Neo4j
 */
@Repository
public interface GenreNodeRepository extends Neo4jRepository<GenreNode, String> {
    
    /**
     * Find genre by name
     */
    Optional<GenreNode> findByName(String name);
    
    /**
     * Delete genre by name
     */
    @Query("MATCH (g:Genre {name: $name}) DETACH DELETE g")
    void deleteByName(@Param("name") String name);
    
    /**
     * Check if genre exists by name
     */
    boolean existsByName(String name);
    
    // ========== LIKES RELATIONSHIP METHODS ==========
    
    /**
     * Check if user likes a genre
     */
    @Query("MATCH (u:User {mongoId: $userId})-[r:LIKES]->(g:Genre {name: $genreName}) RETURN COUNT(r) > 0")
    boolean userLikesGenre(@Param("userId") String userId, @Param("genreName") String genreName);
    
    /**
     * Create LIKES relationship between user and genre
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})
        MATCH (g:Genre {name: $genreName})
        CREATE (u)-[r:LIKES {timestamp: $timestamp}]->(g)
        RETURN r
        """)
    void createLikesRelationship(
        @Param("userId") String userId,
        @Param("genreName") String genreName,
        @Param("timestamp") LocalDateTime timestamp
    );
    
    /**
     * Delete LIKES relationship between user and genre
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[r:LIKES]->(g:Genre {name: $genreName})
        DELETE r
        RETURN COUNT(r)
        """)
    Long deleteLikesRelationship(
        @Param("userId") String userId,
        @Param("genreName") String genreName
    );
    
    /**
     * Get all genres liked by a user
     */
    @Query("""
        MATCH (u:User {mongoId: $userId})-[r:LIKES]->(g:Genre)
        RETURN g.name AS name, r.timestamp AS timestamp
        ORDER BY r.timestamp DESC
        """)
    List<Map<String, Object>> getLikedGenresByUser(@Param("userId") String userId);
    
    // ========== GET OR CREATE METHODS ==========
    
    /**
     * Get or create GenreNode - if not found, creates a new one with the given name
     * This is a default method to centralize the get-or-create pattern used across services
     * Names are normalized to Title Case to prevent duplicates ("fantasy" -> "Fantasy")
     */
    default GenreNode getOrCreate(String name) {
        String normalizedName = NormalizationUtils.normalizeGenreName(name);
        return findByName(normalizedName)
                .orElseGet(() -> {
                    GenreNode newNode = new GenreNode();
                    newNode.setName(normalizedName);
                    return save(newNode);
                });
    }
    
    // ========== ANALYTICS METHODS ==========
    
    /**
     * Find influencers for a specific genre based on review engagement quality
     * Returns users whose reviews consistently receive high engagement
     */
    @Query("""
        MATCH (g:Genre {name: $genreName})<-[:BELONGS_TO]-(b:Book)<-[:REFER_TO]-(r:Review)<-[:POSTED]-(influencer:User)
        MATCH (r)<-[:LIKES]-(fan:User)
        WITH influencer,
             count(DISTINCT r) AS numReviews,
             count(fan) AS totalLikes
        WHERE numReviews > 3
        RETURN influencer.username AS username,
               totalLikes AS totalEngagement,
               numReviews AS numReviews,
               toFloat(totalLikes) / numReviews AS avgLikesPerReview
        ORDER BY avgLikesPerReview DESC
        LIMIT $limit
        """)
    List<InfluencerProjection> findGenreInfluencers(@Param("genreName") String genreName, @Param("limit") int limit);
    
    /**
     * Find top influencers across all genres
     */
    @Query("""
        MATCH (r:Review)<-[:POSTED]-(influencer:User)
        MATCH (r)<-[:LIKES]-(fan:User)
        WITH influencer,
             count(DISTINCT r) AS numReviews,
             count(fan) AS totalLikes
        WHERE numReviews > 5
        RETURN influencer.username AS username,
               totalLikes AS totalEngagement,
               numReviews AS numReviews,
               toFloat(totalLikes) / numReviews AS avgLikesPerReview
        ORDER BY avgLikesPerReview DESC
        LIMIT $limit
        """)
    List<InfluencerProjection> findTopInfluencers(@Param("limit") int limit);
}
