package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.exceptions.*;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.repository.mongo.*;
import it.unipi.bookSphere.repository.neo4j.*;
import it.unipi.bookSphere.repository.neo4j.projections.*;
import it.unipi.bookSphere.service.async.AsyncLikeTasks;
import it.unipi.bookSphere.validation.NormalizationUtils;
import it.unipi.bookSphere.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing likes on books, reviews, authors, and genres
 * Manages both MongoDB (counters) and Neo4j (relationships)
 */
@Service
@RequiredArgsConstructor
public class LikeService {

    private static final Logger logger = LoggerFactory.getLogger(LikeService.class);
    
    // MongoDB repositories
    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final MongoTemplate mongoTemplate;
    
    // Neo4j repositories
    private final UserNodeRepository userNodeRepository;
    private final BookNodeRepository bookNodeRepository;
    private final ReviewNodeRepository reviewNodeRepository;
    private final AuthorNodeRepository authorNodeRepository;
    private final GenreNodeRepository genreNodeRepository;
    
    // Async tasks
    private final AsyncLikeTasks asyncLikeTasks;

    // ========== BOOK LIKES ==========

    /**
     * Like a book - creates LIKES relationship in Neo4j
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UnauthorizedOperationException.class, BookNotFoundException.class, BookArchivedException.class, AlreadyExistsException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void likeBook(String bookId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Validate book exists
        BookDocument book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + bookId));

        //Book is ARCHIVED SHOULD NOT be appear on neo4j but we check to not create a new one
        if(book.getAvailability().equals("ARCHIVED")){
            throw new BookArchivedException("Book is archived: " + bookId);
        }
        
        // Get or create UserNode using repository method
        userNodeRepository.getOrCreate(currentUserId, SecurityUtils.getCurrentUsername(), null);
        
        // Get or create BookNode using repository method
        bookNodeRepository.getOrCreate(bookId, book.getTitle(), book.getPublicationYear());
        
        // Check if already liked using repository method
        boolean alreadyLiked = bookNodeRepository.userLikesBook(currentUserId, bookId);
        
        if (alreadyLiked) {
            throw new AlreadyExistsException("Already liked this book");
        }
        
        // Create LIKES relationship using repository method
        bookNodeRepository.createLikesRelationship(currentUserId, bookId, LocalDateTime.now());
        
        logger.info("User {} liked book {}", currentUserId, bookId);
    }

    /**
     * Unlike a book - removes LIKES relationship from Neo4j
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UnauthorizedOperationException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void unlikeBook(String bookId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Remove LIKES relationship using repository method
        Long deleted = bookNodeRepository.deleteLikesRelationship(currentUserId, bookId);
        
        if (deleted == 0) {
            logger.warn("User {} had not liked book {}", currentUserId, bookId);
        } else {
            logger.info("User {} unliked book {}", currentUserId, bookId);
        }
    }

    // ========== REVIEW LIKES ==========

    /**
     * Like a review - creates LIKES relationship in Neo4j and updates likes_count in MongoDB
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UnauthorizedOperationException.class, ReviewNotFoundException.class, AlreadyExistsException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void likeReview(String reviewId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Validate review exists
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("Review not found with ID: " + reviewId));
        
        // Get or create UserNode using repository method
        userNodeRepository.getOrCreate(currentUserId, SecurityUtils.getCurrentUsername(), null);
        
        // Get ReviewNode
        reviewNodeRepository.findByMongoId(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException("ReviewNode not found in Neo4j"));
        
        // Check if already liked using repository method
        boolean alreadyLiked = reviewNodeRepository.userLikesReview(currentUserId, reviewId);
        
        if (alreadyLiked) {
            throw new AlreadyExistsException("Already liked this review");
        }
        
        // Create LIKES relationship in Neo4j using repository method
        reviewNodeRepository.createLikesRelationship(currentUserId, reviewId, LocalDateTime.now());
        
        // Increment likes_count in MongoDB (eventual consistency)
        Query query = new Query(Criteria.where("_id").is(reviewId));
        Update update = new Update().inc("likes_count", 1);
        mongoTemplate.updateFirst(query, update, Review.class);
        
        // Update popular_reviews_snapshot in book (eventual consistency)
        asyncLikeTasks.updateBookPopularReviews(review.getBookSnapshot().getBookId());
        
        logger.info("User {} liked review {}", currentUserId, reviewId);
    }

    /**
     * Unlike a review - removes LIKES relationship from Neo4j and updates MongoDB
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UnauthorizedOperationException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void unlikeReview(String reviewId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Remove LIKES relationship from Neo4j using repository method
        Long deleted = reviewNodeRepository.deleteLikesRelationship(currentUserId, reviewId);
        
        if (deleted > 0) {
            // Decrement likes_count in MongoDB (eventual consistency)
            Query query = new Query(Criteria.where("_id").is(reviewId));
            Update update = new Update().inc("likes_count", -1);
            mongoTemplate.updateFirst(query, update, Review.class);
            
            // Update popular_reviews_snapshot in book (eventual consistency)
            Review review = reviewRepository.findById(reviewId).orElse(null);
            if (review != null) {
                asyncLikeTasks.updateBookPopularReviews(review.getBookSnapshot().getBookId());
            }
            
            logger.info("User {} unliked review {}", currentUserId, reviewId);
        } else {
            logger.warn("User {} had not liked review {}", currentUserId, reviewId);
        }
    }

    // ========== GENRE LIKES ==========

    /**
     * Like a genre - creates LIKES relationship in Neo4j
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UnauthorizedOperationException.class, AlreadyExistsException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void likeGenre(String genreName) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Normalize genre name for consistency
        String normalizedGenreName = NormalizationUtils.normalizeGenreName(genreName);
        
        // Get or create UserNode using repository method
        userNodeRepository.getOrCreate(currentUserId, SecurityUtils.getCurrentUsername(), null);
        
        // Get or create GenreNode using repository method
        genreNodeRepository.getOrCreate(normalizedGenreName);
        
        // Check if already liked using repository method
        boolean alreadyLiked = genreNodeRepository.userLikesGenre(currentUserId, normalizedGenreName);
        
        if (alreadyLiked) {
            throw new AlreadyExistsException("Already liked this genre");
        }
        
        // Create LIKES relationship using repository method
        genreNodeRepository.createLikesRelationship(currentUserId, normalizedGenreName, LocalDateTime.now());
        
        logger.info("User {} liked genre {}", currentUserId, normalizedGenreName);
    }

    /**
     * Unlike a genre
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UnauthorizedOperationException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void unlikeGenre(String genreName) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Normalize genre name for consistency
        String normalizedGenreName = NormalizationUtils.normalizeGenreName(genreName);
        
        // Remove LIKES relationship using repository method
        Long deleted = genreNodeRepository.deleteLikesRelationship(currentUserId, normalizedGenreName);
        
        if (deleted == 0) {
            logger.warn("User {} had not liked genre {}", currentUserId, normalizedGenreName);
        } else {
            logger.info("User {} unliked genre {}", currentUserId, normalizedGenreName);
        }
    }

    // ========== AUTHOR LIKES ==========

    /**
     * Like an author - creates LIKES relationship in Neo4j
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UnauthorizedOperationException.class, AuthorArchivedException.class, AlreadyExistsException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void likeAuthor(String authorId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Validate author exists in MongoDB
        AuthorDocument author = authorRepository.findById(authorId)
                .orElseThrow(() -> new AuthorNotFoundException("Author not found with ID: " + authorId));

        //Author is ARCHIVED SHOULD NOT appear in neo4j and we dont want to create a new one
        if(author.getStatus().equals("ARCHIVED")){
            throw new AuthorArchivedException("Author archived with ID: " + authorId);
        }
        
        // Get or create UserNode using repository method
        userNodeRepository.getOrCreate(currentUserId, SecurityUtils.getCurrentUsername(), null);
        
        // Get or create AuthorNode using repository method
        authorNodeRepository.getOrCreate(authorId, author.getName());
        
        // Check if already liked using repository method
        boolean alreadyLiked = authorNodeRepository.userLikesAuthor(currentUserId, authorId);
        
        if (alreadyLiked) {
            throw new AlreadyExistsException("Already liked this author");
        }
        
        // Create LIKES relationship using repository method
        authorNodeRepository.createLikesRelationship(currentUserId, authorId, LocalDateTime.now());
        
        logger.info("User {} liked author {}", currentUserId, authorId);
    }

    /**
     * Unlike an author
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UnauthorizedOperationException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void unlikeAuthor(String authorId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Remove LIKES relationship using repository method
        Long deleted = authorNodeRepository.deleteLikesRelationship(currentUserId, authorId);
        
        if (deleted == 0) {
            logger.warn("User {} had not liked author {}", currentUserId, authorId);
        } else {
            logger.info("User {} unliked author {}", currentUserId, authorId);
        }
    }

    // ========== GET LIKED ITEMS ==========

    /**
     * Get all books liked by current user
     */
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<BookDTO> getLikedBooks() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Query Neo4j using repository method
        // Should not return ARCHIVED book
        List<BookLikeProjection> results = bookNodeRepository.getLikedBooksByUser(currentUserId);
        
        List<BookDTO> likedBooks = new ArrayList<>();
        for (BookLikeProjection result : results) {
            BookDTO dto = new BookDTO();
            dto.setId(result.bookId());
            dto.setTitle(result.title());
            likedBooks.add(dto);
        }
        
        return likedBooks;
    }
    
    /**
     * Get all books liked by current user with pagination
     * 
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated list of liked books
     */
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Page<BookDTO> getLikedBooks(int page, int size) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        long skip = (long) page * size;
        
        // Query Neo4j with pagination
        List<BookLikeProjection> results = bookNodeRepository.getLikedBooksByUser(currentUserId, skip, size);
        
        // Get total count
        long total = bookNodeRepository.countLikedBooksByUser(currentUserId);
        
        List<BookDTO> likedBooks = new ArrayList<>();
        for (BookLikeProjection result : results) {
            BookDTO dto = new BookDTO();
            dto.setId(result.bookId());
            dto.setTitle(result.title());
            likedBooks.add(dto);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(likedBooks, pageable, total);
    }

    /**
     * Get all authors liked by current user
     */
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<AuthorDTO> getLikedAuthors() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Query Neo4j using repository method
        // Should not return ARCHIVED author
        List<AuthorLikeProjection> results = authorNodeRepository.getLikedAuthorsByUser(currentUserId);
        
        List<AuthorDTO> likedAuthors = new ArrayList<>();
        for (AuthorLikeProjection result : results) {
            AuthorDTO dto = new AuthorDTO();
            dto.setId(result.authorId());
            dto.setName(result.name());
            likedAuthors.add(dto);
        }
        
        return likedAuthors;
    }
    
    /**
     * Get all authors liked by current user with pagination
     * 
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated list of liked authors
     */
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Page<AuthorDTO> getLikedAuthors(int page, int size) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        long skip = (long) page * size;
        
        // Query Neo4j with pagination
        List<AuthorLikeProjection> results = authorNodeRepository.getLikedAuthorsByUser(currentUserId, skip, size);
        
        // Get total count
        long total = authorNodeRepository.countLikedAuthorsByUser(currentUserId);
        
        List<AuthorDTO> likedAuthors = new ArrayList<>();
        for (AuthorLikeProjection result : results) {
            AuthorDTO dto = new AuthorDTO();
            dto.setId(result.authorId());
            dto.setName(result.name());
            likedAuthors.add(dto);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(likedAuthors, pageable, total);
    }

    /**
     * Get all reviews liked by current user
     */
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<ReviewDTO> getLikedReviews() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Query Neo4j using repository method
        List<ReviewLikeProjection> results = reviewNodeRepository.getLikedReviewsByUser(currentUserId);
        
        // Fetch full review details from MongoDB
        List<String> reviewIds = results.stream()
                .map(ReviewLikeProjection::reviewId)
                .toList();
        
        if (reviewIds.isEmpty()) {
            return List.of();
        }
        
        List<Review> reviews = reviewRepository.findByIdIn(reviewIds);
        
        List<ReviewDTO> likedReviews = new ArrayList<>();
        for (Review review : reviews) {
            ReviewDTO dto = new ReviewDTO();
            dto.setId(review.getId());
            dto.setUsername(review.getUsername());
            dto.setRating(review.getRating());
            dto.setText(review.getText());
            dto.setBookId(review.getBookSnapshot().getBookId());
            dto.setBookTitle(review.getBookSnapshot().getTitle());
            likedReviews.add(dto);
        }
        
        return likedReviews;
    }
    
    /**
     * Get all reviews liked by current user with pagination
     * 
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated list of liked reviews
     */
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Page<ReviewDTO> getLikedReviews(int page, int size) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        long skip = (long) page * size;
        
        // Query Neo4j with pagination
        List<ReviewLikeProjection> results = reviewNodeRepository.getLikedReviewsByUser(currentUserId, skip, size);
        
        // Get total count
        long total = reviewNodeRepository.countLikedReviewsByUser(currentUserId);
        
        // Fetch full review details from MongoDB
        List<String> reviewIds = results.stream()
                .map(ReviewLikeProjection::reviewId)
                .toList();
        
        List<ReviewDTO> likedReviews = new ArrayList<>();
        if (!reviewIds.isEmpty()) {
            List<Review> reviews = reviewRepository.findByIdIn(reviewIds);
            
            for (Review review : reviews) {
                ReviewDTO dto = new ReviewDTO();
                dto.setId(review.getId());
                dto.setUsername(review.getUsername());
                dto.setRating(review.getRating());
                dto.setText(review.getText());
                dto.setBookId(review.getBookSnapshot().getBookId());
                dto.setBookTitle(review.getBookSnapshot().getTitle());
                likedReviews.add(dto);
            }
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(likedReviews, pageable, total);
    }

    /**
     * Get all genres liked by current user
     */
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public List<GenreDTO> getLikedGenres() {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // Query Neo4j using repository method
        List<GenreLikeProjection> results = genreNodeRepository.getLikedGenresByUser(currentUserId);
        
        List<GenreDTO> likedGenres = new ArrayList<>();
        for (GenreLikeProjection result : results) {
            GenreDTO dto = new GenreDTO();
            dto.setName(result.name());
            likedGenres.add(dto);
        }
        
        return likedGenres;
    }
    
    /**
     * Get all genres liked by current user with pagination
     * 
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated list of liked genres
     */
    @Retryable(retryFor = {RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Page<GenreDTO> getLikedGenres(int page, int size) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        long skip = (long) page * size;
        
        // Query Neo4j with pagination
        List<GenreLikeProjection> results = genreNodeRepository.getLikedGenresByUser(currentUserId, skip, size);
        
        // Get total count
        long total = genreNodeRepository.countLikedGenresByUser(currentUserId);
        
        List<GenreDTO> likedGenres = new ArrayList<>();
        for (GenreLikeProjection result : results) {
            GenreDTO dto = new GenreDTO();
            dto.setName(result.name());
            likedGenres.add(dto);
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return new PageImpl<>(likedGenres, pageable, total);
    }

}
