package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.dto.BookSummaryDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for AuthorDocument <-> AuthorDTO conversion
 */
@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthorMapper {

    /**
     * Convert AuthorDocument to AuthorDTO
     */
    AuthorDTO toDTO(AuthorDocument document);

    /**
     * AfterMapping: Calculate average rating from sum and count
     */
    @org.mapstruct.AfterMapping
    default void calculateAuthorAverage(@org.mapstruct.MappingTarget AuthorDTO dto, AuthorDocument document) {
        if (document.getRatingsCount() != null && document.getRatingsCount() > 0 && document.getSumRatings() != null) {
            dto.setAverageRating((double) document.getSumRatings() / document.getRatingsCount());
        } else {
            dto.setAverageRating(0.0);
        }
    }

    /**
     * Convert AuthorDocument.PublishedBook to BookSummaryDTO
     */
    BookSummaryDTO toBookSummaryDTO(AuthorDocument.PublishedBook publishedBook);

    /**
     * Convert AuthorDTO to AuthorDocument (for create/update operations)
     */
    AuthorDocument toDocument(AuthorDTO dto);

    /**
     * Convert BookSummaryDTO to AuthorDocument.PublishedBook
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "title")
    AuthorDocument.PublishedBook toPublishedBook(BookSummaryDTO dto);

    /**
     * Convert list of AuthorDocument.PublishedBook to list of BookSummaryDTO
     */
    List<BookSummaryDTO> toBookSummaryDTOList(List<AuthorDocument.PublishedBook> publishedBooks);

    /**
     * Convert list of BookSummaryDTO to list of AuthorDocument.PublishedBook
     */
    List<AuthorDocument.PublishedBook> toPublishedBookList(List<BookSummaryDTO> dtos);
}
