package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.RecommendationDTO;
import it.unipi.bookSphere.dto.WrappedDTO;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.repository.neo4j.projections.RecommendationProjection;
import it.unipi.bookSphere.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for user-specific features like recommendations and yearly wrap
 */
@Service
@RequiredArgsConstructor
public class UserFeaturesService {

    private static final Logger logger = LoggerFactory.getLogger(UserFeaturesService.class);
    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;
    
    private final MongoTemplate mongoTemplate;
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

    /**
     * Generate yearly wrapped for the current user
     * Simplified version using current user data
     */
    public WrappedDTO getYearlyWrapped() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UserNotFoundException("User not authenticated");
        }
        
        logger.info("Generating yearly wrapped for user: {}", currentUserId);
        
        // Get user data
        Optional<RegisteredUser> userOpt = userRepository.findById(currentUserId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User not found: " + currentUserId);
        }
        
        RegisteredUser user = userOpt.get();
        int currentYear = LocalDate.now().getYear();
        
        // Initialize wrapped data
        WrappedDTO wrapped = new WrappedDTO();
        wrapped.setYear(currentYear);
        
        // Process bookshelf for read books
        List<RegisteredUser.BookshelfItem> readBooks = new ArrayList<>();
        if (user.getBookshelf() != null) {
            readBooks = user.getBookshelf().stream()
                .filter(item -> "read".equals(item.getStatus()))
                .filter(item -> item.getAddedAt() != null && 
                    item.getAddedAt().atZone(ZoneOffset.UTC).getYear() == currentYear)
                .collect(Collectors.toList());
        }
        
        wrapped.setTotalBooksRead(readBooks.size());
        
        // Process reviews for best/worst books
        if (user.getReviewsYear() != null && !user.getReviewsYear().isEmpty()) {
            List<RegisteredUser.ReviewYear> sortedReviews = user.getReviewsYear().stream()
                .sorted((a, b) -> Integer.compare(b.getRating(), a.getRating()))
                .collect(Collectors.toList());
            
            if (!sortedReviews.isEmpty()) {
                RegisteredUser.ReviewYear best = sortedReviews.get(0);
                wrapped.setBestBook(new WrappedDTO.BookSummaryWithRatingDTO(
                    best.getId(),
                    best.getBook(),
                    "Unknown Author", // Simple version - could be enhanced
                    best.getRating()
                ));
                
                RegisteredUser.ReviewYear worst = sortedReviews.get(sortedReviews.size() - 1);
                wrapped.setWorstBook(new WrappedDTO.BookSummaryWithRatingDTO(
                    worst.getId(),
                    worst.getBook(),
                    "Unknown Author",
                    worst.getRating()
                ));
            }
        }
        
        // Calculate top authors from read books
        Map<String, Integer> authorFreq = new HashMap<>();
        for (RegisteredUser.BookshelfItem book : readBooks) {
            if (book.getAuthor() != null && book.getAuthor().getName() != null) {
                authorFreq.put(book.getAuthor().getName(),
                    authorFreq.getOrDefault(book.getAuthor().getName(), 0) + 1);
            }
        }
        
        List<WrappedDTO.AuthorFrequencyDTO> topAuthors = authorFreq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(3)
            .map(entry -> new WrappedDTO.AuthorFrequencyDTO(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        wrapped.setTopAuthors(topAuthors);
        
        // Calculate top genres from read books
        Map<String, Integer> genreFreq = new HashMap<>();
        for (RegisteredUser.BookshelfItem book : readBooks) {
            if (book.getGenres() != null) {
                for (String genre : book.getGenres()) {
                    genreFreq.put(genre, genreFreq.getOrDefault(genre, 0) + 1);
                }
            }
        }
        
        List<WrappedDTO.GenreFrequencyDTO> topGenres = genreFreq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(3)
            .map(entry -> new WrappedDTO.GenreFrequencyDTO(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        wrapped.setTopGenres(topGenres);
        
        logger.info("Generated yearly wrapped for user {} with {} books read", currentUserId, wrapped.getTotalBooksRead());
        return wrapped;
    }
}
