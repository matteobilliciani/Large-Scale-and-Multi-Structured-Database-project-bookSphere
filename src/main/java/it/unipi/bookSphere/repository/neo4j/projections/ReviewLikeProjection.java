package it.unipi.bookSphere.repository.neo4j.projections;

import java.time.LocalDateTime;

/**
 * Projection record for Neo4j query results when fetching liked reviews
 */
public record ReviewLikeProjection(
    String reviewId,
    Integer rating,
    LocalDateTime timestamp
) {}
