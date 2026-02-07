package it.unipi.bookSphere.repository.neo4j.projections;

/**
 * DTO for influencer query results
 */
public record InfluencerProjection(
    String username,
    Long totalEngagement,
    Long numReviews,
    Double avgLikesPerReview
) {}
