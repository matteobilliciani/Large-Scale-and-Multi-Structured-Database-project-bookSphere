package it.unipi.bookSphere.service;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.exceptions.AuthorArchivedException;
import it.unipi.bookSphere.exceptions.AuthorNotFoundException;
import it.unipi.bookSphere.mapper.AuthorMapper;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling author operations
 */
@Service
@RequiredArgsConstructor
public class AuthorService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorService.class);
    
    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;

    /**
     * Find author by ID
     * 
     * @param id MongoDB ObjectId of the author
     * @return AuthorDTO with author information
     * @throws AuthorNotFoundException if author is not found
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public AuthorDTO findById(String id) {
        logger.info("Finding author by id: {}", id);
        
        AuthorDocument author = authorRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Author not found with id: {}", id);
                    return new AuthorNotFoundException("Author not found with id: " + id);
                });

        if(author.getStatus().equals("ARCHIVED")){
            logger.warn("Author ARCHIVED with id: {}", id);
            throw new AuthorArchivedException("Author ARCHIVED " + id);
        }
        
        AuthorDTO authorDTO = authorMapper.toDTO(author);
        logger.info("Author found: {}", author.getName());
        return authorDTO;
    }

    /**
     * Search authors by name (partial matching)
     * 
     * @param name Author name to search
     * @return List of matching authors
     */
    @Retryable(
        retryFor = {RuntimeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<AuthorDTO> searchByName(String name) {
        logger.info("Searching authors by name: {}", name);
        
        List<AuthorDocument> authors = authorRepository.findByNameContainingIgnoreCase(name);

        authors.removeIf(author->author.getStatus().equals("ARCHIVED"));
        
        logger.info("Found {} authors matching '{}'", authors.size(), name);
        return authors.stream()
                .map(authorMapper::toDTO)
                .collect(Collectors.toList());
    }
}
