package it.unipi.bookSphere.repository.neo4j.projections;

import java.time.LocalDateTime;

/**
 * Projection record for Neo4j query results when fetching liked books
 */
public record BookLikeProjection(
    String bookId,
    String title,
    LocalDateTime timestamp
) {}
