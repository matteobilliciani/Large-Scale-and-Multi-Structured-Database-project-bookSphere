package it.unipi.bookSphere.repository.mongo.projections;

import lombok.Data;

@Data
public class BookTrendProjection {
    private String id;
    private String title;
    private String author;
    private Integer startYear;
    private Integer endYear;
    private Double startRating;
    private Double endRating;
    private Double ratingDelta;
}
