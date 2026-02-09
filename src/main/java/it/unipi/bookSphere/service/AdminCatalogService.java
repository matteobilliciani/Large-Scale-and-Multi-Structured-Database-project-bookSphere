package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.dto.GenreDTO;
import it.unipi.bookSphere.exceptions.AuthorArchivedException;
import it.unipi.bookSphere.exceptions.AuthorNotFoundException;
import it.unipi.bookSphere.mapper.AuthorMapper;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.neo4j.AuthorNode;
import it.unipi.bookSphere.model.neo4j.GenreNode;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.neo4j.AuthorNodeRepository;
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
 * Admin service for managing authors and genres in the catalog.
 * Implements strict consistency between MongoDB and Neo4j for admin operations.
 */
@Service
@RequiredArgsConstructor
public class AdminCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(AdminCatalogService.class);
    
    private final AuthorRepository authorRepository;
    private final AuthorNodeRepository authorNodeRepository;
    private final GenreNodeRepository genreNodeRepository;
    private final AuthorMapper authorMapper;

    // ========== AUTHOR MANAGEMENT ==========

    /**
     * Add a new author to the system.
     * 
     * STRICT CONSISTENCY (Synchronous):
     * - MONGO: Insert in authors collection
     * - NEO4J: Create (:Author) Node
     * 
     * EVENTUAL CONSISTENCY:
     * - None
     * 
     * @param authorDTO Author details
     * @return Created AuthorDTO
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public AuthorDTO addAuthor(AuthorDTO authorDTO) {
        logger.info("Admin adding new author: {}", authorDTO.getName());
        
        // Normalize author name for consistency
        String normalizedName = NormalizationUtils.normalizeAuthorName(authorDTO.getName());
        authorDTO.setName(normalizedName);
        
        // 1. Create author in MongoDB
        AuthorDocument authorDocument = authorMapper.toDocument(authorDTO);
        //authorDocument.setStatus("ACTIVE");
        
        // Initialize statistics
        if (authorDocument.getRatingsCount() == null) {
            authorDocument.setRatingsCount(0);
        }
        if (authorDocument.getSumRatings() == null) {
            authorDocument.setSumRatings(0);
        }
        if (authorDocument.getPublishedBooks() == null) {
            authorDocument.setPublishedBooks(new ArrayList<>());
        }
        
        AuthorDocument savedAuthor = authorRepository.save(authorDocument);
        logger.info("Author created in MongoDB with ID: {}", savedAuthor.getId());
        
        try {
            // 2. Create or get AuthorNode in Neo4j (centralized in repository)
            AuthorNode authorNode = authorNodeRepository.getOrCreate(
                savedAuthor.getId(),
                savedAuthor.getName()
            );
            logger.info("Author node ready in Neo4j: {}", authorNode.getName());
            
        } catch (Exception e) {
            // Rollback MongoDB if Neo4j operations fail
            logger.error("Failed to create author in Neo4j, rolling back MongoDB", e);
            authorRepository.delete(savedAuthor);
            throw new RuntimeException("Failed to create author: " + e.getMessage(), e);
        }
        
        return authorMapper.toDTO(savedAuthor);
    }

    /**
     * Update author information.
     * 
     * STRICT CONSISTENCY (Synchronous):
     * - MONGO: Update document master in authors collection
     * - NEO4J: Update property a.name on node
     * 
     * EVENTUAL CONSISTENCY:
     * - NONE (embedded names in books and reviews remain unchanged)
     * 
     * @param id Author MongoDB ObjectId
     * @param authorDTO Updated author details
     * @return Updated AuthorDTO
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public AuthorDTO updateAuthor(String id, AuthorDTO authorDTO) {
        logger.info("Admin updating author with ID: {}", id);
        
        // Normalize author name for consistency
        if (authorDTO.getName() != null) {
            String normalizedName = NormalizationUtils.normalizeAuthorName(authorDTO.getName());
            authorDTO.setName(normalizedName);
        }
        
        // 1. Find existing author in MongoDB
        AuthorDocument existingAuthor = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException("Author not found with ID: " + id));

        // If author is ARCHIVED cannot be updated
        if(existingAuthor.getStatus().equals("ARCHIVED"))
            throw new AuthorArchivedException("Author is archived cannot be updated with ID: " + id);   
        
        // 2. Update MongoDB document
        if (authorDTO.getName() != null) {
            existingAuthor.setName(authorDTO.getName());
        }
        
        AuthorDocument updatedAuthor = authorRepository.save(existingAuthor);
        logger.info("Author updated in MongoDB: {}", updatedAuthor.getName());
        
        try {
            // 3. Update AuthorNode name in Neo4j
            AuthorNode authorNode = authorNodeRepository.findByMongoId(id)
                    .orElseThrow(() -> new AuthorNotFoundException("Author node not found in Neo4j with ID: " + id));
            
            if (authorDTO.getName() != null) {
                authorNode.setName(authorDTO.getName());
            }
            
            authorNodeRepository.save(authorNode);
            logger.info("Author node updated in Neo4j");
            
        } catch (Exception e) {
            logger.error("Failed to update author in Neo4j", e);
            throw new RuntimeException("Failed to update author in Neo4j: " + e.getMessage(), e);
        }
        
        return authorMapper.toDTO(updatedAuthor);
    }

    /**
     * Soft delete an author from the system.
     * 
     * STRICT CONSISTENCY (Synchronous):
     * - MONGO: Set status: "ARCHIVED" in authors collection
     * - NEO4J: DETACH DELETE author node
     * 
     * EVENTUAL CONSISTENCY:
     * - None
     * 
     * @param id Author MongoDB ObjectId
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void deleteAuthor(String id) {
        logger.info("Admin archiving author with ID: {}", id);
        
        // 1. Soft delete in MongoDB (set status to ARCHIVED)
        AuthorDocument author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException("Author not found with ID: " + id));

        // If author already ARCHIVED
        if(author.getStatus().equals("ARCHIVED"))
            throw new AuthorArchivedException("Author is already ARCHIVED with ID: " + id);  
        
        author.setStatus("ARCHIVED");
        authorRepository.save(author);
        logger.info("Author archived in MongoDB: {}", author.getName());
        
        try {
            // 2. DETACH DELETE in Neo4j (removes node and all relationships)
            authorNodeRepository.deleteByMongoId(id);
            logger.info("Author node deleted from Neo4j (DETACH DELETE)");
            
        } catch (Exception e) {
            logger.error("Failed to delete author from Neo4j", e);
            throw new RuntimeException("Failed to delete author from Neo4j: " + e.getMessage(), e);
        }
    }

    // ========== GENRE MANAGEMENT ==========

    /**
     * Add a new genre to the system.
     * 
     * STRICT CONSISTENCY (Synchronous):
     * - NEO4J: Create (:Genre) Node
     * 
     * EVENTUAL CONSISTENCY:
     * - None
     * 
     * NOTE: Genres are stored only in Neo4j for graph-based recommendations
     * 
     * @param genreDTO Genre details
     * @return Created GenreDTO
     */
    @Transactional
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public GenreDTO addGenre(GenreDTO genreDTO) {
        logger.info("Admin adding new genre: {}", genreDTO.getName());
        
        // Normalize genre name for consistency
        String normalizedName = NormalizationUtils.normalizeGenreName(genreDTO.getName());
        
        // Check if genre already exists
        if (genreNodeRepository.existsByName(normalizedName)) {
            logger.warn("Genre already exists: {}", normalizedName);
            throw new IllegalArgumentException("Genre already exists: " + normalizedName);
        }
        
        // Create GenreNode in Neo4j (centralized in repository) - will auto-normalize
        GenreNode genreNode = genreNodeRepository.getOrCreate(normalizedName);
        logger.info("Genre node ready in Neo4j: {}", genreNode.getName());
        
        // Return with normalized name
        genreDTO.setName(genreNode.getName());
        return genreDTO;
    }
}
