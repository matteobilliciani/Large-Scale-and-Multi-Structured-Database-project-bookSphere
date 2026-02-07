package it.unipi.bookSphere.repository.neo4j.projections;

/**
 * Projection for user recommendation query results
 */
public record RecommendationProjection(
    String bookId,
    String title,
    Integer publicationYear,
    Long score,
    String author
) {}
