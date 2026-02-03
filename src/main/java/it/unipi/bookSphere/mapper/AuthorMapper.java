package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.dto.BookSummaryDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuthorMapper {

    /**
     * Convert AuthorDocument to AuthorDTO
     */
    public AuthorDTO toDTO(AuthorDocument document) {
        if (document == null) {
            return null;
        }

        AuthorDTO dto = new AuthorDTO();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setAverageRating(document.getAverageRating());
        dto.setRatingsCount(document.getRatingsCount());
        dto.setSumRatings(document.getSumRatings());

        // Map published books
        if (document.getPublishedBooks() != null) {
            List<BookSummaryDTO> publishedBooks = document.getPublishedBooks().stream()
                    .map(this::toBookSummaryDTO)
                    .collect(Collectors.toList());
            dto.setPublishedBooks(publishedBooks);
        } else {
            dto.setPublishedBooks(Collections.emptyList());
        }

        return dto;
    }

    /**
     * Convert AuthorDocument.PublishedBook to BookSummaryDTO
     */
    private BookSummaryDTO toBookSummaryDTO(AuthorDocument.PublishedBook publishedBook) {
        if (publishedBook == null) {
            return null;
        }

        BookSummaryDTO dto = new BookSummaryDTO();
        dto.setId(publishedBook.getId());
        dto.setTitle(publishedBook.getTitle());
        return dto;
    }

    /**
     * Convert AuthorDTO to AuthorDocument (for create/update operations)
     */
    public AuthorDocument toDocument(AuthorDTO dto) {
        if (dto == null) {
            return null;
        }

        AuthorDocument document = new AuthorDocument();
        document.setId(dto.getId());
        document.setName(dto.getName());
        document.setAverageRating(dto.getAverageRating());
        document.setRatingsCount(dto.getRatingsCount());
        document.setSumRatings(dto.getSumRatings());

        // Map published books
        if (dto.getPublishedBooks() != null) {
            List<AuthorDocument.PublishedBook> publishedBooks = dto.getPublishedBooks().stream()
                    .map(this::toPublishedBook)
                    .collect(Collectors.toList());
            document.setPublishedBooks(publishedBooks);
        }

        return document;
    }

    /**
     * Convert BookSummaryDTO to AuthorDocument.PublishedBook
     */
    private AuthorDocument.PublishedBook toPublishedBook(BookSummaryDTO dto) {
        if (dto == null) {
            return null;
        }

        return new AuthorDocument.PublishedBook(dto.getId(), dto.getTitle());
    }
}
