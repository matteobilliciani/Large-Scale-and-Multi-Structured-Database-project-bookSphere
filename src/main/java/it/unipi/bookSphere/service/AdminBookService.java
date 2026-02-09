package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.BookDTO;
import it.unipi.bookSphere.exceptions.BookArchivedException;
import it.unipi.bookSphere.exceptions.BookNotFoundException;
import it.unipi.bookSphere.mapper.BookMapper;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.neo4j.AuthorNode;
import it.unipi.bookSphere.model.neo4j.BookNode;
import it.unipi.bookSphere.model.neo4j.GenreNode;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.neo4j.AuthorNodeRepository;
import it.unipi.bookSphere.repository.neo4j.BookNodeRepository;
import it.unipi.bookSphere.repository.neo4j.GenreNodeRepository;
import it.unipi.bookSphere.utils.NormalizationUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Admin service for managing books in the catalog.
 * Implements strict consistency between MongoDB and Neo4j for admin operations.
 */
@Service
@RequiredArgsConstructor
public class AdminBookService {

    private static final Logger logger = LoggerFactory.getLogger(AdminBookService.class);
    
    private final BookRepository bookRepository;
    private final BookNodeRepository bookNodeRepository;
    private final AuthorNodeRepository authorNodeRepository;
    private final GenreNodeRepository genreNodeRepository;
    private final BookMapper bookMapper;

    /**
     * Add a new book to the catalog.
     * 
     * STRICT CONSISTENCY (Synchronous):
     * - MONGO: Insert in books collection
     * - NEO4J: Create (:Book) Node + (:Author)-[:WROTE]->(:Book) relationship
     * 
     * EVENTUAL CONSISTENCY:
     * - None
     * 
     * @param bookDTO Book details
     * @return Created BookDTO
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public BookDTO addBook(BookDTO bookDTO) {
        logger.info("Admin adding new book: {}", bookDTO.getTitle());
        
        // Normalize book title and author name for consistency
        if (bookDTO.getTitle() != null) {
            bookDTO.setTitle(NormalizationUtils.normalizeBookTitle(bookDTO.getTitle()));
        }
        if (bookDTO.getAuthor() != null && bookDTO.getAuthor().getName() != null) {
            bookDTO.getAuthor().setName(NormalizationUtils.normalizeAuthorName(bookDTO.getAuthor().getName()));
        }
        
        // 1. Create book in MongoDB
        BookDocument bookDocument = bookMapper.toDocument(bookDTO);
        //bookDocument.setStatus("ACTIVE");
        bookDocument.setSource("admin");
        
        // Initialize empty lists if not present
        if (bookDocument.getReviews() == null) {
            bookDocument.setReviews(new ArrayList<>());
        }
        if (bookDocument.getStatsPerYear() == null) {
            bookDocument.setStatsPerYear(new ArrayList<>());
        }

        bookDocument.setAvailability("ACTIVE");
        
        BookDocument savedBook = bookRepository.save(bookDocument);
        logger.info("Book created in MongoDB with ID: {}", savedBook.getId());
        
        try {
            // 2. Create or get AuthorNode in Neo4j (centralized in repository)
            AuthorNode authorNode = authorNodeRepository.getOrCreate(
                bookDTO.getAuthor().getId(),
                bookDTO.getAuthor().getName()
            );
            logger.info("Author node ready in Neo4j: {}", authorNode.getName());
            
            // 3. Create or get BookNode in Neo4j (centralized in repository)
            BookNode bookNode = bookNodeRepository.getOrCreate(
                savedBook.getId(),
                savedBook.getTitle(),
                savedBook.getPublicationYear()
            );
            logger.info("Book node ready in Neo4j: {}", bookNode.getTitle());
            
            // 4. Create WROTE relationship between Author and Book
            authorNodeRepository.createWroteRelationship(authorNode.getMongoId(), bookNode.getMongoId());
            logger.info("WROTE relationship created between author and book");
            
            // 5. Create BELONGS_TO relationships with genres if present
            if (bookDTO.getGenres() != null && !bookDTO.getGenres().isEmpty()) {
                for (String genreName : bookDTO.getGenres()) {
                    try {
                        // Get or create genre node (centralized in repository)
                        GenreNode genreNode = genreNodeRepository.getOrCreate(genreName);
                        // Create BELONGS_TO relationship
                        bookNodeRepository.createBelongsToRelationship(bookNode.getMongoId(), genreNode.getName());
                    } catch (Exception e) {
                        logger.warn("Failed to create BELONGS_TO relationship for genre: {}", genreName, e);
                    }
                }
                logger.info("BELONGS_TO relationships created with {} genres", bookDTO.getGenres().size());
            }
            
        } catch (Exception e) {
            // Rollback MongoDB if Neo4j operations fail
            logger.error("Failed to create book in Neo4j, rolling back MongoDB", e);
            bookRepository.delete(savedBook);
            throw new RuntimeException("Failed to create book: " + e.getMessage(), e);
        }
        
        return bookMapper.toDTO(savedBook);
    }

    /**
     * Update book information.
     * 
     * STRICT CONSISTENCY (Synchronous):
     * - MONGO: Update document master in books collection
     * - NEO4J: Update property b.title on node
     * 
     * EVENTUAL CONSISTENCY:
     * - NONE (copies of title in reviews and bookshelves remain as historical data)
     * 
     * @param id Book MongoDB ObjectId
     * @param bookDTO Updated book details
     * @return Updated BookDTO
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},        
        noRetryFor = {BookNotFoundException.class, BookArchivedException.class},        
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public BookDTO updateBook(String id, BookDTO bookDTO) {
        logger.info("Admin updating book with ID: {}", id);
        
        // Normalize book title and author name for consistency
        if (bookDTO.getTitle() != null) {
            bookDTO.setTitle(NormalizationUtils.normalizeBookTitle(bookDTO.getTitle()));
        }
        if (bookDTO.getAuthor() != null && bookDTO.getAuthor().getName() != null) {
            bookDTO.getAuthor().setName(NormalizationUtils.normalizeAuthorName(bookDTO.getAuthor().getName()));
        }
        
        // 1. Find existing book in MongoDB
        BookDocument existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));

        //IF book is archived can t be updated
        if(existingBook.getAvailability().equals("ARCHIVED")){
            throw new BookArchivedException("Book is archived with ID: " + id);
        }

        // Track if author or genres have changed to update relationships
        boolean authorChanged = false;
        boolean genresChanged = false;
        
        // 2. Update MongoDB document
        if (bookDTO.getTitle() != null) {
            existingBook.setTitle(bookDTO.getTitle());
        }
        if (bookDTO.getPublicationYear() != null) {
            existingBook.setPublicationYear(bookDTO.getPublicationYear());
        }
        if (bookDTO.getDescription() != null) {
            existingBook.setDescription(bookDTO.getDescription());
        }
        if (bookDTO.getAuthor() != null) {
            // Check if author has changed
            if (existingBook.getAuthor() == null || 
                !existingBook.getAuthor().getId().equals(bookDTO.getAuthor().getId())) {
                authorChanged = true;
            }
            BookDocument.Author authorInfo = new BookDocument.Author();
            authorInfo.setId(bookDTO.getAuthor().getId());
            authorInfo.setName(bookDTO.getAuthor().getName());
            existingBook.setAuthor(authorInfo);
        }
        if (bookDTO.getGenres() != null) {
            genresChanged = true; // Simplified: assume genres changed if provided
            existingBook.setGenres(bookDTO.getGenres());
        }
        if (bookDTO.getIsbns() != null) {
            BookDocument.ExternalIds externalIds = new BookDocument.ExternalIds();
            externalIds.setIsbns(bookDTO.getIsbns());
            existingBook.setExternalIds(externalIds);
        }
        
        BookDocument updatedBook = bookRepository.save(existingBook);
        logger.info("Book updated in MongoDB: {}", updatedBook.getTitle());
        
        try {
            // 3. Update BookNode title in Neo4j
            BookNode bookNode = bookNodeRepository.findByMongoId(id)
                    .orElseThrow(() -> new BookNotFoundException("Book node not found in Neo4j with ID: " + id));
            
            if (bookDTO.getTitle() != null) {
                bookNode.setTitle(bookDTO.getTitle());
            }
            if (bookDTO.getPublicationYear() != null) {
                bookNode.setYear(bookDTO.getPublicationYear());
            }
            
            bookNodeRepository.save(bookNode);
            logger.info("Book node updated in Neo4j");
            
            // 4. Update WROTE relationship if author changed
            if (authorChanged) {
                logger.info("Author changed, updating WROTE relationship");
                // Delete old WROTE relationships
                Long deletedCount = authorNodeRepository.deleteWroteRelationshipsForBook(id);
                logger.info("Deleted {} old WROTE relationships", deletedCount);
                
                // Create or get new author node
                AuthorNode newAuthorNode = authorNodeRepository.getOrCreate(
                    updatedBook.getAuthor().getId(),
                    updatedBook.getAuthor().getName()
                );
                
                // Create new WROTE relationship
                authorNodeRepository.createWroteRelationship(newAuthorNode.getMongoId(), id);
                logger.info("Created new WROTE relationship with author: {}", newAuthorNode.getName());
            }
            
            // 5. Update BELONGS_TO relationships if genres changed
            if (genresChanged && updatedBook.getGenres() != null) {
                logger.info("Genres changed, updating BELONGS_TO relationships");
                // Delete old BELONGS_TO relationships
                Long deletedCount = bookNodeRepository.deleteBelongsToRelationshipsForBook(id);
                logger.info("Deleted {} old BELONGS_TO relationships", deletedCount);
                
                // Create new BELONGS_TO relationships
                for (String genreName : updatedBook.getGenres()) {
                    try {
                        GenreNode genreNode = genreNodeRepository.getOrCreate(genreName);
                        bookNodeRepository.createBelongsToRelationship(id, genreNode.getName());
                    } catch (Exception e) {
                        logger.warn("Failed to create BELONGS_TO relationship for genre: {}", genreName, e);
                    }
                }
                logger.info("Created {} new BELONGS_TO relationships", updatedBook.getGenres().size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to update book in Neo4j", e);
            throw new RuntimeException("Failed to update book in Neo4j: " + e.getMessage(), e);
        }
        
        return bookMapper.toDTO(updatedBook);
    }

    /**
     * Soft delete a book from the catalog.
     * 
     * STRICT CONSISTENCY (Synchronous):
     * - MONGO: Set status: "ARCHIVED" in books collection
     * - NEO4J: DETACH DELETE book node (to block recommendations)
     * 
     * EVENTUAL CONSISTENCY:
     * - NONE (book remains visible in user libraries and existing reviews)
     * 
     * @param id Book MongoDB ObjectId
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        noRetryFor = {BookNotFoundException.class, BookArchivedException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteBook(String id) {
        logger.info("Admin archiving book with ID: {}", id);
        
        // 1. Soft delete in MongoDB (set status to ARCHIVED)
        BookDocument book = bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found with ID: " + id));

        //Book already archived
        if(book.getAvailability().equals("ARCHIVED")){
            throw new BookArchivedException("Book is archived with ID: " + id);
        }
        
        book.setAvailability("ARCHIVED");
        bookRepository.save(book);
        logger.info("Book archived in MongoDB: {}", book.getTitle());
        
        try {
            // 2. DETACH DELETE in Neo4j (removes node and all relationships)
            bookNodeRepository.deleteByMongoId(id);
            logger.info("Book node deleted from Neo4j (DETACH DELETE)");
            
        } catch (Exception e) {
            logger.error("Failed to delete book from Neo4j", e);
            throw new RuntimeException("Failed to delete book from Neo4j: " + e.getMessage(), e);
        }
    }

}
