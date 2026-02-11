package it.unipi.bookSphere.service;

import it.unipi.bookSphere.exceptions.AlreadyExistsException;
import it.unipi.bookSphere.exceptions.BookArchivedException;
import it.unipi.bookSphere.exceptions.BookNotFoundException;
import it.unipi.bookSphere.exceptions.UnauthorizedOperationException;
import it.unipi.bookSphere.exceptions.UserNotFoundException;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.utils.SecurityUtils;
import it.unipi.bookSphere.validation.ValidBookshelfStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Service for managing user bookshelf
 */
@Service
@RequiredArgsConstructor
@Validated
public class BookshelfService {

    private static final Logger logger = LoggerFactory.getLogger(BookshelfService.class);
    
    private final RegisteredUserRepository userRepository;
    private final BookRepository bookRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Add book to user's bookshelf
     * Status can be: "to_read", "reading", "read"
     * Status validation  is handled by @ValidBookshelfStatus
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {UnauthorizedOperationException.class, BookNotFoundException.class, BookArchivedException.class, UserNotFoundException.class, AlreadyExistsException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void addBookToBookshelf(String bookId, @ValidBookshelfStatus String status) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Validate status
        //see @Validated
        /* List<String> validStatuses = Arrays.asList("to_read", "reading", "read");
        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException("Invalid status. Must be one of: to_read, reading, read");
        }*/
        
        // 2. Validate book exists
        BookDocument book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + bookId));

        if(book.getAvailability().equals("ARCHIVED")){
            throw new BookArchivedException("Book is archived: " + bookId);
        }
        
        // 3. Validate user exists
        RegisteredUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + currentUserId));
        
        // 4. Check if book already in bookshelf
        if (user.getBookshelf() != null) {
            boolean alreadyExists = user.getBookshelf().stream()
                    .anyMatch(item -> item.getBookId().equals(bookId));
            if (alreadyExists) {
                throw new AlreadyExistsException("Book already in bookshelf");
            }
        }
        
        // 5. Create BookshelfItem with extended reference pattern (denormalization)
        RegisteredUser.BookshelfItem item = new RegisteredUser.BookshelfItem();
        item.setBookId(bookId);
        item.setStatus(status);
        item.setAddedAt(Instant.now());
        item.setTitle(book.getTitle());
        
        // Set author snapshot
        if (book.getAuthor() != null) {
            RegisteredUser.Author author = new RegisteredUser.Author();
            author.setId(book.getAuthor().getId());
            author.setName(book.getAuthor().getName());
            item.setAuthor(author);
        }
        
        // Set genres
        item.setGenres(book.getGenres());
        
        // 6. Add to bookshelf
        Query query = new Query(Criteria.where("_id").is(currentUserId));
        Update update = new Update().addToSet("bookshelf", item);
        
        mongoTemplate.updateFirst(query, update, RegisteredUser.class);
        logger.info("Added book {} to bookshelf for user {}", bookId, currentUserId);
    }

    /**
     * Update book status in bookshelf
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},        
        noRetryFor = {UnauthorizedOperationException.class, BookNotFoundException.class, UserNotFoundException.class},        
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void updateBookStatus(String bookId, String newStatus) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Validate status
        List<String> validStatuses = Arrays.asList("to_read", "reading", "read");
        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status. Must be one of: to_read, reading, read");
        }
        
        // 2. Validate user exists
        RegisteredUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + currentUserId));
        
        // 3. Check if book exists in bookshelf
        if (user.getBookshelf() == null || user.getBookshelf().stream()
                .noneMatch(item -> item.getBookId().equals(bookId))) {
            throw new BookNotFoundException("Book not found in bookshelf");
        }
        
        // 4. Update status
        Query query = new Query(Criteria.where("_id").is(currentUserId)
                .and("bookshelf.book_id").is(bookId));
        Update update = new Update().set("bookshelf.$.status", newStatus);
        
        mongoTemplate.updateFirst(query, update, RegisteredUser.class);
        logger.info("Updated book {} status to {} for user {}", bookId, newStatus, currentUserId);
    }

    /**
     * Remove book from bookshelf
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},       
        noRetryFor = {UnauthorizedOperationException.class},        
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void removeBookFromBookshelf(String bookId) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        
        if (currentUserId == null) {
            throw new UnauthorizedOperationException("User not authenticated");
        }
        
        // 1. Validate user exists and book is in bookshelf
        RegisteredUser user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + currentUserId));
        
        // 2. Check if book exists in bookshelf
        if (user.getBookshelf() == null || user.getBookshelf().stream()
                .noneMatch(item -> item.getBookId().equals(bookId))) {
            throw new BookNotFoundException("Book not found in bookshelf");
        }
        
        // 3. Remove book from bookshelf
        Query query = new Query(Criteria.where("_id").is(currentUserId));
        Update update = new Update().pull("bookshelf", 
                Query.query(Criteria.where("book_id").is(bookId)));
        
        mongoTemplate.updateFirst(query, update, RegisteredUser.class);
        logger.info("Removed book {} from bookshelf for user {}", bookId, currentUserId);
    }
}
