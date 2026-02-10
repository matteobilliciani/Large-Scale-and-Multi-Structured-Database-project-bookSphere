package it.unipi.bookSphere.repository.neo4j.projections;

import java.time.LocalDateTime;

/**
 * Projection record for Neo4j query results when fetching followed users
 * Records work better than interfaces with Spring Data Neo4j custom queries
 */
public record UserFollowProjection(
    String userId,
    String username,
    String country,
    LocalDateTime since
) {}
