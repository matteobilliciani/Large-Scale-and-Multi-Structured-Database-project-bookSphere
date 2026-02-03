package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    /**
     * Convert RegisteredUser to UserDTO
     */
    public UserDTO toDTO(RegisteredUser document) {
        if (document == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(document.getId());
        dto.setUsername(document.getUsername());
        dto.setEmail(document.getEmail());
        dto.setCountry(document.getCountry());
        dto.setStatus(document.getStatus());

        // Convert Instant to LocalDateTime
        if (document.getJoinedAt() != null) {
            dto.setJoinedAt(LocalDateTime.ofInstant(document.getJoinedAt(), ZoneId.systemDefault()));
        }

        // Map bookshelf
        if (document.getBookshelf() != null) {
            dto.setBookshelf(
                    document.getBookshelf().stream()
                            .map(this::toBookshelfItemDTO)
                            .collect(Collectors.toList())
            );
        } else {
            dto.setBookshelf(Collections.emptyList());
        }

        // Map reviews year
        if (document.getReviewsYear() != null) {
            dto.setReviewsYear(
                    document.getReviewsYear().stream()
                            .map(this::toReviewYearDTO)
                            .collect(Collectors.toList())
            );
        } else {
            dto.setReviewsYear(Collections.emptyList());
        }

        return dto;
    }

    /**
     * Convert RegisteredUser.BookshelfItem to BookshelfItemDTO
     */
    private BookshelfItemDTO toBookshelfItemDTO(RegisteredUser.BookshelfItem item) {
        if (item == null) {
            return null;
        }

        BookshelfItemDTO dto = new BookshelfItemDTO();
        dto.setBookId(item.getBookId());
        dto.setTitle(item.getTitle());
        dto.setStatus(item.getStatus());
        dto.setGenres(item.getGenres());

        // Convert Instant to LocalDateTime
        if (item.getAddedAt() != null) {
            dto.setAddedAt(LocalDateTime.ofInstant(item.getAddedAt(), ZoneId.systemDefault()));
        }

        // Map author
        if (item.getAuthor() != null) {
            AuthorDTO authorDTO = new AuthorDTO();
            authorDTO.setId(item.getAuthor().getId());
            authorDTO.setName(item.getAuthor().getName());
            dto.setAuthor(authorDTO);
        }

        return dto;
    }

    /**
     * Convert RegisteredUser.ReviewYear to ReviewYearDTO
     */
    private ReviewYearDTO toReviewYearDTO(RegisteredUser.ReviewYear reviewYear) {
        if (reviewYear == null) {
            return null;
        }

        ReviewYearDTO dto = new ReviewYearDTO();
        dto.setId(reviewYear.getId());
        dto.setRating(reviewYear.getRating());
        dto.setBook(reviewYear.getBook());
        return dto;
    }

    /**
     * Convert UserDTO to RegisteredUser (for create/update operations)
     */
    public RegisteredUser toDocument(UserDTO dto) {
        if (dto == null) {
            return null;
        }

        RegisteredUser document = new RegisteredUser();
        document.setId(dto.getId());
        document.setUsername(dto.getUsername());
        document.setEmail(dto.getEmail());
        document.setCountry(dto.getCountry());
        document.setStatus(dto.getStatus());

        // Convert LocalDateTime to Instant
        if (dto.getJoinedAt() != null) {
            document.setJoinedAt(dto.getJoinedAt().atZone(ZoneId.systemDefault()).toInstant());
        }

        // Map bookshelf
        if (dto.getBookshelf() != null) {
            document.setBookshelf(
                    dto.getBookshelf().stream()
                            .map(this::toBookshelfItem)
                            .collect(Collectors.toList())
            );
        }

        // Map reviews year
        if (dto.getReviewsYear() != null) {
            document.setReviewsYear(
                    dto.getReviewsYear().stream()
                            .map(this::toReviewYear)
                            .collect(Collectors.toList())
            );
        }

        return document;
    }

    /**
     * Convert BookshelfItemDTO to RegisteredUser.BookshelfItem
     */
    private RegisteredUser.BookshelfItem toBookshelfItem(BookshelfItemDTO dto) {
        if (dto == null) {
            return null;
        }

        RegisteredUser.BookshelfItem item = new RegisteredUser.BookshelfItem();
        item.setBookId(dto.getBookId());
        item.setTitle(dto.getTitle());
        item.setStatus(dto.getStatus());
        item.setGenres(dto.getGenres());

        // Convert LocalDateTime to Instant
        if (dto.getAddedAt() != null) {
            item.setAddedAt(dto.getAddedAt().atZone(ZoneId.systemDefault()).toInstant());
        }

        // Map author
        if (dto.getAuthor() != null) {
            RegisteredUser.Author author = new RegisteredUser.Author();
            author.setId(dto.getAuthor().getId());
            author.setName(dto.getAuthor().getName());
            item.setAuthor(author);
        }

        return item;
    }

    /**
     * Convert ReviewYearDTO to RegisteredUser.ReviewYear
     */
    private RegisteredUser.ReviewYear toReviewYear(ReviewYearDTO dto) {
        if (dto == null) {
            return null;
        }

        RegisteredUser.ReviewYear reviewYear = new RegisteredUser.ReviewYear();
        reviewYear.setId(dto.getId());
        reviewYear.setRating(dto.getRating());
        reviewYear.setBook(dto.getBook());
        return reviewYear;
    }
}
