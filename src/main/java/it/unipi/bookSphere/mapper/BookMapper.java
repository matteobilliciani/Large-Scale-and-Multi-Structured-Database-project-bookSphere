package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import org.mapstruct.*;

import java.time.*;
import java.util.List;

/**
 * MapStruct mapper for BookDocument <-> BookDTO conversion
 */
@Mapper(componentModel = "spring")
public interface BookMapper {

    /**
     * Convert BookDocument to BookDTO
     */
    @Mapping(target = "isbns", source = "externalIds.isbns")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalRatingsCount", ignore = true)
    @Mapping(target = "totalSumRating", ignore = true)
    @Mapping(target = "trendScore", source = "monthScore")
    BookDTO toDTO(BookDocument document);

    /**
     * AfterMapping: Calculate overall statistics from yearly stats
     */
    @AfterMapping
    default void calculateOverallStats(@MappingTarget BookDTO dto, BookDocument document) {
        if (document.getStatsPerYear() != null && !document.getStatsPerYear().isEmpty()) {
            int totalRatingsCount = 0;
            int totalSumRating = 0;

            for (BookDocument.YearStat yearStat : document.getStatsPerYear()) {
                if (yearStat.getRatingsCount() != null) {
                    totalRatingsCount += yearStat.getRatingsCount();
                }
                if (yearStat.getSumRating() != null) {
                    totalSumRating += yearStat.getSumRating();
                }
            }

            dto.setTotalRatingsCount(totalRatingsCount);
            dto.setTotalSumRating(totalSumRating);

            // Calculate average rating
            if (totalRatingsCount > 0) {
                dto.setAverageRating((double) totalSumRating / totalRatingsCount);
            }
        }
    }

    /**
     * Convert BookDocument.Author to AuthorDTO
     */
    @Mapping(target = "publishedBooks", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "ratingsCount", ignore = true)
    @Mapping(target = "sumRatings", ignore = true)
    AuthorDTO toAuthorDTO(BookDocument.Author author);

    /**
     * Convert BookDocument.ReviewSnapshot to ReviewSnapshotDTO
     */
    @Mapping(target = "date", source = "date", qualifiedByName = "instantToString")
    @Mapping(target = "text", ignore = true)
    @Mapping(target = "summary", ignore = true)
    ReviewSnapshotDTO toReviewSnapshotDTO(BookDocument.ReviewSnapshot snapshot);

    /**
     * Convert BookDocument.YearStat to StatsPerYearDTO
     */
    StatsPerYearDTO toStatsPerYearDTO(BookDocument.YearStat yearStat);

    /**
     * Convert BookDocument.MonthScore to TrendScoreDTO
     */
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    TrendScoreDTO toTrendScoreDTO(BookDocument.MonthScore monthScore);

    /**
     * Convert BookDTO to BookDocument (for create/update operations)
     */
    @Mapping(target = "externalIds.isbns", source = "isbns")
    @Mapping(target = "monthScore", source = "trendScore")
    BookDocument toDocument(BookDTO dto);

    /**
     * Convert AuthorDTO to BookDocument.Author
     */
    BookDocument.Author toBookAuthor(AuthorDTO dto);

    /**
     * Convert ReviewSnapshotDTO to BookDocument.ReviewSnapshot
     */
    @Mapping(target = "date", source = "date", qualifiedByName = "stringToInstant")
    BookDocument.ReviewSnapshot toReviewSnapshot(ReviewSnapshotDTO dto);

    /**
     * Convert StatsPerYearDTO to BookDocument.YearStat
     */
    BookDocument.YearStat toYearStat(StatsPerYearDTO dto);

    /**
     * Convert TrendScoreDTO to BookDocument.MonthScore
     */
    @Mapping(target = "ratingCount", ignore = true)
    @Mapping(target = "sumRating", ignore = true)
    @Mapping(target = "currentMonth", ignore = true)
    BookDocument.MonthScore toMonthScore(TrendScoreDTO dto);

    /**
     * Convert BookDocument to BookSummaryDTO (simplified version)
     */
    BookSummaryDTO toSummaryDTO(BookDocument document);

    /**
     * Convert list mappings
     */
    List<ReviewSnapshotDTO> toReviewSnapshotDTOList(List<BookDocument.ReviewSnapshot> snapshots);
    List<BookDocument.ReviewSnapshot> toReviewSnapshotList(List<ReviewSnapshotDTO> dtos);
    List<StatsPerYearDTO> toStatsPerYearDTOList(List<BookDocument.YearStat> yearStats);
    List<BookDocument.YearStat> toYearStatList(List<StatsPerYearDTO> dtos);

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

    /**
     * Convert Instant to String
     */
    @Named("instantToString")
    default String instantToString(Instant instant) {
        return instant != null ? instant.toString() : null;
    }

    /**
     * Convert String to Instant
     */
    @Named("stringToInstant")
    default Instant stringToInstant(String date) {
        return date != null ? Instant.parse(date) : null;
    }
}
