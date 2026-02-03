package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * MapStruct mapper for RegisteredUser <-> UserDTO conversion
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Convert RegisteredUser to UserDTO
     */
    @Mapping(target = "joinedAt", source = "joinedAt", qualifiedByName = "instantToLocalDateTime")
    UserDTO toDTO(RegisteredUser document);

    /**
     * Convert RegisteredUser.BookshelfItem to BookshelfItemDTO
     */
    @Mapping(target = "addedAt", source = "addedAt", qualifiedByName = "instantToLocalDateTime")
    BookshelfItemDTO toBookshelfItemDTO(RegisteredUser.BookshelfItem item);

    /**
     * Convert RegisteredUser.ReviewYear to ReviewYearDTO
     */
    ReviewYearDTO toReviewYearDTO(RegisteredUser.ReviewYear reviewYear);

    /**
     * Convert UserDTO to RegisteredUser (for create/update operations)
     */
    @Mapping(target = "joinedAt", source = "joinedAt", qualifiedByName = "localDateTimeToInstant")
    @Mapping(target = "passwordHashed", ignore = true)
    RegisteredUser toDocument(UserDTO dto);

    /**
     * Convert BookshelfItemDTO to RegisteredUser.BookshelfItem
     */
    @Mapping(target = "addedAt", source = "addedAt", qualifiedByName = "localDateTimeToInstant")
    RegisteredUser.BookshelfItem toBookshelfItem(BookshelfItemDTO dto);

    /**
     * Convert ReviewYearDTO to RegisteredUser.ReviewYear
     */
    RegisteredUser.ReviewYear toReviewYear(ReviewYearDTO dto);

    /**
     * Convert AuthorDTO to RegisteredUser.Author
     */
    RegisteredUser.Author toUserAuthor(AuthorDTO dto);

    /**
     * Convert RegisteredUser.Author to AuthorDTO
     */
    @Mapping(target = "publishedBooks", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "ratingsCount", ignore = true)
    @Mapping(target = "sumRatings", ignore = true)
    AuthorDTO toAuthorDTO(RegisteredUser.Author author);

    /**
     * Convert list of BookshelfItemDTO to list of BookshelfItem
     */
    List<RegisteredUser.BookshelfItem> toBookshelfItemList(List<BookshelfItemDTO> dtos);

    /**
     * Convert list of BookshelfItem to list of BookshelfItemDTO
     */
    List<BookshelfItemDTO> toBookshelfItemDTOList(List<RegisteredUser.BookshelfItem> items);

    /**
     * Convert list of ReviewYearDTO to list of ReviewYear
     */
    List<RegisteredUser.ReviewYear> toReviewYearList(List<ReviewYearDTO> dtos);

    /**
     * Convert list of ReviewYear to list of ReviewYearDTO
     */
    List<ReviewYearDTO> toReviewYearDTOList(List<RegisteredUser.ReviewYear> reviewYears);

    /**
     * Convert Instant to LocalDateTime
     */
    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }

    /**
     * Convert LocalDateTime to Instant
     */
    @Named("localDateTimeToInstant")
    default Instant localDateTimeToInstant(LocalDateTime localDateTime) {
        return localDateTime != null ? localDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;
    }
}
