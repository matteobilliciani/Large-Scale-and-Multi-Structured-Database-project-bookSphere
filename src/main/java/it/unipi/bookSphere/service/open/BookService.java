package it.unipi.bookSphere.service.open;

import it.unipi.bookSphere.dto.BookDTO;
import it.unipi.bookSphere.exceptions.BookArchivedException;
import it.unipi.bookSphere.exceptions.BookNotFoundException;
import it.unipi.bookSphere.mapper.BookMapper;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling book operations
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);
    
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    /**
     * Find book by ID
     * 
     * @param id MongoDB ObjectId of the book
     * @return BookDTO with book information including snapshot reviews and statistics
     * @throws BookNotFoundException if book is not found
     */
    @Retryable(
        retryFor = {RuntimeException.class},        
        noRetryFor = {BookNotFoundException.class, BookArchivedException.class},        
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public BookDTO findById(String id) {
        logger.info("Finding book by id: {}", id);
        
        BookDocument book = bookRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Book not found with id: {}", id);
                    return new BookNotFoundException("Book not found with id: " + id);
                });
        
        if(book.getAvailability().equals("ARCHIVED")){
            logger.warn("Book ARCHIVED with id: {}", id);
            throw new BookArchivedException("Book is archived with id: " + id);
        }
        
        BookDTO bookDTO = bookMapper.toDTO(book);
        logger.info("Book found: {}", book.getTitle());
        return bookDTO;
    }

    /**
     * Search books by title (partial matching)
     * 
     * @param title Book title to search
     * @return List of matching books
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<BookDTO> searchByTitle(String title) {
        logger.info("Searching books by title: {}", title);
        
        List<BookDocument> books = bookRepository.findByTitleContainingIgnoreCase(title);

        books.removeIf(book->book.getAvailability().equals("ARCHIVED"));
        
        logger.info("Found {} books matching '{}'", books.size(), title);
        return books.stream()
                .map(bookMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Search books by title with pagination (partial matching)
     * 
     * @param title Book title to search
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Page of matching books
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Page<BookDTO> searchByTitle(String title, int page, int size) {
        logger.info("Searching books by title: {} (page: {}, size: {})", title, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BookDocument> books;
        
        // If title is null or empty, return all active books
        if (title == null || title.trim().isEmpty()) {
            books = bookRepository.findByAvailabilityNot("ARCHIVED", pageable);
        } else {
            // Use text search for longer queries with complete words (better performance)
            // Use regex match for short queries, partial words, or prefix matching (better accuracy)
            String trimmedTitle = title.trim();
            boolean isPrefix = trimmedTitle.endsWith("_") || trimmedTitle.endsWith("-");
            boolean hasCompleteWords = trimmedTitle.contains(" ") && trimmedTitle.split("\\s+").length > 1;
            boolean isLongEnough = trimmedTitle.length() >= 4;
            boolean useTextSearch = isLongEnough && (hasCompleteWords || (!isPrefix && trimmedTitle.matches(".*[a-zA-Z]{3,}.*")));
            
            if (useTextSearch) {
                try {
                    books = bookRepository.searchByText(trimmedTitle, "ARCHIVED", pageable);
                    // If text search returns no results for a reasonable query, try with regex as fallback
                    if (books.isEmpty() && trimmedTitle.length() <= 15) {
                        logger.debug("Text search returned no results, falling back to regex for: {}", trimmedTitle);
                        books = bookRepository.findByTitleContainingIgnoreCaseAndAvailabilityNot(trimmedTitle, "ARCHIVED", pageable);
                    }
                } catch (Exception e) {
                    logger.warn("Text search failed, falling back to regex: {}", e.getMessage());
                    books = bookRepository.findByTitleContainingIgnoreCaseAndAvailabilityNot(trimmedTitle, "ARCHIVED", pageable);
                }
            } else {
                // For short queries, prefixes, or partial matches, use regex matching for accuracy
                books = bookRepository.findByTitleContainingIgnoreCaseAndAvailabilityNot(trimmedTitle, "ARCHIVED", pageable);
            }
        }
        
        // Map to DTO and filter is applied by repository/database level for better performance
        Page<BookDTO> result = books.map(bookMapper::toDTO);
        
        logger.info("Found {} books matching '{}' on page {}", result.getNumberOfElements(), title, page);
        return result;
    }
}
