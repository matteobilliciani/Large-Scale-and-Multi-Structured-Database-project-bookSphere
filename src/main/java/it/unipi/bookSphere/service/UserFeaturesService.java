package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.RecommendationDTO;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.repository.neo4j.projections.RecommendationProjection;
import it.unipi.bookSphere.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for user-specific features like recommendations and yearly wrap
 */
@Service
@RequiredArgsConstructor
public class UserFeaturesService {

    private static final Logger logger = LoggerFactory.getLogger(UserFeaturesService.class);
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;
    
    private final UserNodeRepository userNodeRepository;
    private final RegisteredUserRepository userRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    /**
     * Get personalized book recommendations for the current user
     * Uses Neo4j graph to find books based on:
     * - Books liked by followed users
     * - Books by liked authors
     * - Books in liked genres
     * Excludes books already reviewed by the user
     * 
     * @param limit Maximum number of recommendations (default 10)
     * @return List of RecommendationDTO with scores
     */
    public List<RecommendationDTO> getRecommendations(Integer limit) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UserNotFoundException("User not authenticated");
        }
        
        logger.info("Getting recommendations for user: {}", currentUserId);
        
        // Verify user exists
        if (!userRepository.existsById(currentUserId)) {
            throw new UserNotFoundException("User not found: " + currentUserId);
        }
        
        int resultLimit = (limit != null && limit > 0) ? limit : DEFAULT_RECOMMENDATION_LIMIT;
        
        // Get recommendations from Neo4j
        List<RecommendationProjection> results = userNodeRepository.getUserRecommendations(currentUserId, resultLimit);
        
        // Convert to DTOs with additional book info from MongoDB
        List<RecommendationDTO> recommendations = results.stream()
            .map(proj -> {
                String bookId = proj.bookId();
                String title = proj.title();
                Integer year = proj.publicationYear();
                Long score = proj.score();
                
                // Try to get additional info from MongoDB
                String authorName = null;
                try {
                    BookDocument book = bookRepository.findById(bookId).orElse(null);
                    if (book != null && book.getAuthor() != null) {
                        String authorId = book.getAuthor().getId();
                        if (authorId != null) {
                            AuthorDocument author = authorRepository.findById(authorId).orElse(null);
                            if (author != null) {
                                authorName = author.getName();
                            }
                        }
                        // Use the year from MongoDB if not available from Neo4j
                        if (year == null) {
                            year = book.getPublicationYear();
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not fetch author info for book {}: {}", bookId, e.getMessage());
                }
                
                return new RecommendationDTO(bookId, title, score, authorName, year);
            })
            .collect(Collectors.toList());
        
        logger.info("Generated {} recommendations for user {}", recommendations.size(), currentUserId);
        return recommendations;
    }
}
