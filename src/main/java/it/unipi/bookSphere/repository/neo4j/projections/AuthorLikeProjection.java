package it.unipi.bookSphere.repository.neo4j.projections;

import java.time.LocalDateTime;

/**
 * Projection record for Neo4j query results when fetching liked authors
 */
public record AuthorLikeProjection(
    String authorId,
    String name,
    LocalDateTime timestamp
) {}
