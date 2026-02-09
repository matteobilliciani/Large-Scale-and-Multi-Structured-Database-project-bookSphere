package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.BookDTO;
import it.unipi.bookSphere.exceptions.BookNotFoundException;
import it.unipi.bookSphere.mapper.BookMapper;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            throw new BookNotFoundException("Book not found with id: " + id);
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
}
