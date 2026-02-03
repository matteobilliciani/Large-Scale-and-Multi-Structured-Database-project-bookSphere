package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.dto.BookSummaryDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for AuthorDocument <-> AuthorDTO conversion
 */
@Mapper(componentModel = "spring")
public interface AuthorMapper {

    /**
     * Convert AuthorDocument to AuthorDTO
     */
    AuthorDTO toDTO(AuthorDocument document);

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
