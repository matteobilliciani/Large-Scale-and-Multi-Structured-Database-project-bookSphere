package it.unipi.bookSphere.repository.neo4j.projections;

import java.time.LocalDateTime;

/**
 * Projection record for Neo4j query results when fetching liked genres
 */
public record GenreLikeProjection(
    String name,
    LocalDateTime timestamp
) {}
