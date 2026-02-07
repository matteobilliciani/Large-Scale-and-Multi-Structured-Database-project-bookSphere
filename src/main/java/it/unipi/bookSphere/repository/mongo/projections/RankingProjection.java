package it.unipi.bookSphere.repository.mongo.projections;

import lombok.Data;

@Data
public class RankingProjection {
    
    private String id;          // Mappa automaticamente _id
    private String name;        // Mappa il titolo
    private Double averageRating;
    private Long totalRatings;
    private String additionalInfo; // Nome autore
}
