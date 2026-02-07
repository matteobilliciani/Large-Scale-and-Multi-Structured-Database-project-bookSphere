package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.InfluencerDTO;
import it.unipi.bookSphere.dto.InternationalityDTO;
import it.unipi.bookSphere.exceptions.BookNotFoundException;
import it.unipi.bookSphere.exceptions.AuthorNotFoundException;
import it.unipi.bookSphere.exceptions.GenreNotFoundException;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.neo4j.AuthorNodeRepository;
import it.unipi.bookSphere.repository.neo4j.BookNodeRepository;
import it.unipi.bookSphere.repository.neo4j.GenreNodeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for analytics operations using Neo4j
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private static final int DEFAULT_INFLUENCER_LIMIT = 10;
    
    private final BookNodeRepository bookNodeRepository;
    private final AuthorNodeRepository authorNodeRepository;
    private final GenreNodeRepository genreNodeRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    /**
     * Calculate Internationality Index for a book or author
     * Analyzes geographical distribution of likes and reviews
     * 
     * @param entityId MongoDB ObjectId of the book or author
     * @param entityType Type of entity: "BOOK" or "AUTHOR"
     * @return List of InternationalityDTO with country distribution
     */
    public List<InternationalityDTO> calculateInternationality(String entityId, String entityType) {
        logger.info("Calculating internationality for entity: {} of type: {}", entityId, entityType);
        
        List<? extends it.unipi.bookSphere.repository.neo4j.projections.InternationalityProjection> results;
        
        if ("BOOK".equalsIgnoreCase(entityType)) {
            logger.debug("Entity {} is a Book", entityId);
            // Verify book exists in MongoDB
            if (!bookRepository.existsById(entityId)) {
                throw new BookNotFoundException("Book not found in MongoDB: " + entityId);
            }
            // Verify book exists in Neo4j
            if (!bookNodeRepository.existsByMongoId(entityId)) {
                throw new BookNotFoundException("Book not found in Neo4j graph: " + entityId);
            }
            results = bookNodeRepository.calculateBookInternationality(entityId);
        } else if ("AUTHOR".equalsIgnoreCase(entityType)) {
            logger.debug("Entity {} is an Author", entityId);
            // Verify author exists in MongoDB
            if (!authorRepository.existsById(entityId)) {
                throw new AuthorNotFoundException("Author not found in MongoDB: " + entityId);
            }
            // Verify author exists in Neo4j
            if (!authorNodeRepository.existsByMongoId(entityId)) {
                throw new AuthorNotFoundException("Author not found in Neo4j graph: " + entityId);
            }
            results = authorNodeRepository.calculateAuthorInternationality(entityId);
        } else {
            throw new IllegalArgumentException("Invalid entityType. Must be 'BOOK' or 'AUTHOR', got: " + entityType);
        }
        
        // Convert results to DTOs
        List<InternationalityDTO> internationality = results.stream()
            .map(proj -> new InternationalityDTO(
                proj.country(),
                proj.uniqueUsers(),
                proj.totalInteractions()
            ))
            .collect(Collectors.toList());
        
        logger.info("Calculated internationality for entity {}: {} countries", entityId,internationality.size());
        return internationality;
    }

    /**
     * Find influencers for a specific genre or across all genres
     * Identifies users whose reviews consistently receive high engagement
     * 
     * @param genre Optional genre name to filter influencers. If null, returns top influencers across all genres
     * @param limit Maximum number of influencers to return (default 10)
     * @return List of InfluencerDTO with engagement metrics
     */
    public List<InfluencerDTO> getInfluencers(String genre, Integer limit) {
        int resultLimit = (limit != null && limit > 0) ? limit : DEFAULT_INFLUENCER_LIMIT;
        
        List<? extends it.unipi.bookSphere.repository.neo4j.projections.InfluencerProjection> results;
        
        if (genre != null && !genre.isEmpty()) {
            logger.info("Finding influencers for genre: {}", genre);
            
            // Verify genre exists
            if (!genreNodeRepository.existsByName(genre)) {
                throw new GenreNotFoundException("Genre not found: " + genre);
            }
            
            results = genreNodeRepository.findGenreInfluencers(genre, resultLimit);
        } else {
            logger.info("Finding top influencers across all genres");
            results = genreNodeRepository.findTopInfluencers(resultLimit);
        }
        
        // Convert results to DTOs
        List<InfluencerDTO> influencers = results.stream()
            .map(proj -> new InfluencerDTO(
                proj.username(),
                proj.totalEngagement(),
                proj.numReviews(),
                proj.avgLikesPerReview()
            ))
            .collect(Collectors.toList());
        
        logger.info("Found {} influencers", influencers.size());
        return influencers;
    }
}
