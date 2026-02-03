package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.model.mongodb.Review;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class ReviewMapper {

    /**
     * Convert Review to ReviewDTO
     */
    public ReviewDTO toDTO(Review document) {
        if (document == null) {
            return null;
        }

        ReviewDTO dto = new ReviewDTO();
        dto.setId(document.getId());
        dto.setRating(document.getRating());
        dto.setText(document.getText());
        dto.setSummary(document.getSummary());
        dto.setLikesCount(document.getLikesCount());
        dto.setSource(document.getSource());
        dto.setIsBanned(document.getIsBanned());

        // Convert Instant to LocalDateTime
        if (document.getCreatedAt() != null) {
            dto.setCreatedAt(LocalDateTime.ofInstant(document.getCreatedAt(), ZoneId.systemDefault()));
        }

        // Map book snapshot
        if (document.getBookSnapshot() != null) {
            dto.setBookId(document.getBookSnapshot().getBookId());
            dto.setBookTitle(document.getBookSnapshot().getTitle());
        }

        return dto;
    }

    /**
     * Convert ReviewDTO to Review (for create/update operations)
     */
    public Review toDocument(ReviewDTO dto) {
        if (dto == null) {
            return null;
        }

        Review document = new Review();
        document.setId(dto.getId());
        document.setRating(dto.getRating());
        document.setText(dto.getText());
        document.setSummary(dto.getSummary());
        document.setLikesCount(dto.getLikesCount());
        document.setSource(dto.getSource());
        document.setIsBanned(dto.getIsBanned());

        // Convert LocalDateTime to Instant
        if (dto.getCreatedAt() != null) {
            document.setCreatedAt(dto.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
        }

        // Map book snapshot
        if (dto.getBookId() != null && dto.getBookTitle() != null) {
            Review.BookSnapshot bookSnapshot = new Review.BookSnapshot();
            bookSnapshot.setBookId(dto.getBookId());
            bookSnapshot.setTitle(dto.getBookTitle());
            document.setBookSnapshot(bookSnapshot);
        }

        return document;
    }
}
