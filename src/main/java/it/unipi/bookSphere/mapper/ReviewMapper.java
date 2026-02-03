package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.model.mongodb.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * MapStruct mapper for Review <-> ReviewDTO conversion
 */
@Mapper(componentModel = "spring")
public interface ReviewMapper {

    /**
     * Convert Review to ReviewDTO
     */
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToLocalDateTime")
    @Mapping(target = "bookId", source = "bookSnapshot.bookId")
    @Mapping(target = "bookTitle", source = "bookSnapshot.title")
    @Mapping(target = "authorName", ignore = true)
    ReviewDTO toDTO(Review document);

    /**
     * Convert ReviewDTO to Review (for create/update operations)
     */
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "localDateTimeToInstant")
    @Mapping(target = "bookSnapshot.bookId", source = "bookId")
    @Mapping(target = "bookSnapshot.title", source = "bookTitle")
    @Mapping(target = "userId", ignore = true)
    Review toDocument(ReviewDTO dto);

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
