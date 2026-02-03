package it.unipi.bookSphere.repository.neo4j;

import it.unipi.bookSphere.model.neo4j.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
