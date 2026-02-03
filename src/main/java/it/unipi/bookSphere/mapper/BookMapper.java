package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookMapper {

    /**
     * Convert BookDocument to BookDTO
     */
    public BookDTO toDTO(BookDocument document) {
        if (document == null) {
            return null;
        }

        BookDTO dto = new BookDTO();
        dto.setId(document.getId());
        dto.setTitle(document.getTitle());
        dto.setPublicationYear(document.getPublicationYear());
        dto.setDescription(document.getDescription());
        dto.setGenres(document.getGenres());
        dto.setSource(document.getSource());

        // Map author
        if (document.getAuthor() != null) {
            dto.setAuthor(toAuthorDTO(document.getAuthor()));
        }

        // Map ISBNs
        if (document.getExternalIds() != null && document.getExternalIds().getIsbns() != null) {
            dto.setIsbns(document.getExternalIds().getIsbns());
        }

        // Map recent reviews snapshot
        if (document.getRecentReviewsSnapshot() != null) {
            dto.setRecentReviewsSnapshot(
                    document.getRecentReviewsSnapshot().stream()
                            .map(this::toReviewSnapshotDTO)
                            .collect(Collectors.toList())
            );
        }

        // Map popular reviews snapshot
        if (document.getPopularReviewsSnapshot() != null) {
            dto.setPopularReviewsSnapshot(
                    document.getPopularReviewsSnapshot().stream()
                            .map(this::toReviewSnapshotDTO)
                            .collect(Collectors.toList())
            );
        }

        // Map stats per year
        if (document.getStatsPerYear() != null) {
            dto.setStatsPerYear(
                    document.getStatsPerYear().stream()
                            .map(this::toStatsPerYearDTO)
                            .collect(Collectors.toList())
            );

            // Calculate overall statistics
            calculateOverallStats(document.getStatsPerYear(), dto);
        }

        // Map trend score
        if (document.getTrendScore() != null) {
            dto.setTrendScore(toTrendScoreDTO(document.getTrendScore()));
        }

        return dto;
    }

    /**
     * Convert BookDocument.Author to AuthorDTO
     */
    private AuthorDTO toAuthorDTO(BookDocument.Author author) {
        if (author == null) {
            return null;
        }

        AuthorDTO dto = new AuthorDTO();
        dto.setId(author.getId());
        dto.setName(author.getName());
        return dto;
    }

    /**
     * Convert BookDocument.ReviewSnapshot to ReviewSnapshotDTO
     */
    private ReviewSnapshotDTO toReviewSnapshotDTO(BookDocument.ReviewSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        ReviewSnapshotDTO dto = new ReviewSnapshotDTO();
        dto.setId(snapshot.getId());
        dto.setUsername(snapshot.getUsername());
        dto.setRating(snapshot.getRating());
        dto.setSnippet(snapshot.getSnippet());
        dto.setNumOfLike(snapshot.getNumOfLike());

        // Convert Instant to String for date
        if (snapshot.getDate() != null) {
            dto.setDate(snapshot.getDate().toString());
        }

        return dto;
    }

    /**
     * Convert BookDocument.YearStat to StatsPerYearDTO
     */
    private StatsPerYearDTO toStatsPerYearDTO(BookDocument.YearStat yearStat) {
        if (yearStat == null) {
            return null;
        }

        StatsPerYearDTO dto = new StatsPerYearDTO();
        dto.setYear(yearStat.getYear());
        dto.setAverageRating(yearStat.getAverageRating());
        dto.setRatingsCount(yearStat.getRatingsCount());
        dto.setSumRating(yearStat.getSumRating());
        return dto;
    }

    /**
     * Convert BookDocument.TrendScore to TrendScoreDTO
     */
    private TrendScoreDTO toTrendScoreDTO(BookDocument.TrendScore trendScore) {
        if (trendScore == null) {
            return null;
        }

        TrendScoreDTO dto = new TrendScoreDTO();
        dto.setRating(trendScore.getRating());

        // Convert Instant to LocalDateTime
        if (trendScore.getUpdatedAt() != null) {
            dto.setUpdatedAt(LocalDateTime.ofInstant(trendScore.getUpdatedAt(), ZoneId.systemDefault()));
        }

        return dto;
    }

    /**
     * Calculate overall statistics from yearly stats
     */
    private void calculateOverallStats(List<BookDocument.YearStat> statsPerYear, BookDTO dto) {
        int totalRatingsCount = 0;
        int totalSumRating = 0;

        for (BookDocument.YearStat yearStat : statsPerYear) {
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

    /**
     * Convert BookDTO to BookDocument (for create/update operations)
     */
    public BookDocument toDocument(BookDTO dto) {
        if (dto == null) {
            return null;
        }

        BookDocument document = new BookDocument();
        document.setId(dto.getId());
        document.setTitle(dto.getTitle());
        document.setPublicationYear(dto.getPublicationYear());
        document.setDescription(dto.getDescription());
        document.setGenres(dto.getGenres());
        document.setSource(dto.getSource());

        // Map author
        if (dto.getAuthor() != null) {
            BookDocument.Author author = new BookDocument.Author();
            author.setId(dto.getAuthor().getId());
            author.setName(dto.getAuthor().getName());
            document.setAuthor(author);
        }

        // Map ISBNs
        if (dto.getIsbns() != null && !dto.getIsbns().isEmpty()) {
            BookDocument.ExternalIds externalIds = new BookDocument.ExternalIds();
            externalIds.setIsbns(dto.getIsbns());
            document.setExternalIds(externalIds);
        }

        // Map recent reviews snapshot
        if (dto.getRecentReviewsSnapshot() != null) {
            document.setRecentReviewsSnapshot(
                    dto.getRecentReviewsSnapshot().stream()
                            .map(this::toReviewSnapshot)
                            .collect(Collectors.toList())
            );
        }

        // Map popular reviews snapshot
        if (dto.getPopularReviewsSnapshot() != null) {
            document.setPopularReviewsSnapshot(
                    dto.getPopularReviewsSnapshot().stream()
                            .map(this::toReviewSnapshot)
                            .collect(Collectors.toList())
            );
        }

        // Map stats per year
        if (dto.getStatsPerYear() != null) {
            document.setStatsPerYear(
                    dto.getStatsPerYear().stream()
                            .map(this::toYearStat)
                            .collect(Collectors.toList())
            );
        }

        // Map trend score
        if (dto.getTrendScore() != null) {
            document.setTrendScore(toTrendScore(dto.getTrendScore()));
        }

        return document;
    }

    /**
     * Convert ReviewSnapshotDTO to BookDocument.ReviewSnapshot
     */
    private BookDocument.ReviewSnapshot toReviewSnapshot(ReviewSnapshotDTO dto) {
        if (dto == null) {
            return null;
        }

        BookDocument.ReviewSnapshot snapshot = new BookDocument.ReviewSnapshot();
        snapshot.setId(dto.getId());
        snapshot.setUsername(dto.getUsername());
        snapshot.setRating(dto.getRating());
        snapshot.setSnippet(dto.getSnippet());
        snapshot.setNumOfLike(dto.getNumOfLike());

        // Convert String to Instant for date
        if (dto.getDate() != null) {
            snapshot.setDate(Instant.parse(dto.getDate()));
        }

        return snapshot;
    }

    /**
     * Convert StatsPerYearDTO to BookDocument.YearStat
     */
    private BookDocument.YearStat toYearStat(StatsPerYearDTO dto) {
        if (dto == null) {
            return null;
        }

        BookDocument.YearStat yearStat = new BookDocument.YearStat();
        yearStat.setYear(dto.getYear());
        yearStat.setAverageRating(dto.getAverageRating());
        yearStat.setRatingsCount(dto.getRatingsCount());
        yearStat.setSumRating(dto.getSumRating());
        return yearStat;
    }

    /**
     * Convert TrendScoreDTO to BookDocument.TrendScore
     */
    private BookDocument.TrendScore toTrendScore(TrendScoreDTO dto) {
        if (dto == null) {
            return null;
        }

        BookDocument.TrendScore trendScore = new BookDocument.TrendScore();
        trendScore.setRating(dto.getRating());

        // Convert LocalDateTime to Instant
        if (dto.getUpdatedAt() != null) {
            trendScore.setUpdatedAt(dto.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant());
        }

        return trendScore;
    }

    /**
     * Convert BookDocument to BookSummaryDTO (simplified version)
     */
    public BookSummaryDTO toSummaryDTO(BookDocument document) {
        if (document == null) {
            return null;
        }

        BookSummaryDTO dto = new BookSummaryDTO();
        dto.setId(document.getId());
        dto.setTitle(document.getTitle());
        return dto;
    }
}
