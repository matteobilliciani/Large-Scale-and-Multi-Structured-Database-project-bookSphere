package it.unipi.bookSphere.repository.neo4j.projections;

/**
 * DTO for internationality query results
 */
public record InternationalityProjection(
    String country,
    Long uniqueUsers,
    Long totalInteractions
) {}
