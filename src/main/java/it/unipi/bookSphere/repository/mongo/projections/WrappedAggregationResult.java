package it.unipi.bookSphere.repository.mongo.projections;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;

/**
 * Classe "specchio" della query MongoDB.
 * I nomi dei campi qui devono corrispondere ESATTAMENTE 
 * ai nomi usati nel $project della query ($facet).
 */
@Data
public class WrappedAggregationResult {

    // Corrisponde al ramo .as("top_authors")
    private List<CountEntry> top_authors;

    // Corrisponde al ramo .as("top_genres")
    private List<CountEntry> top_genres;

    // Corrisponde al ramo .as("meta")
    // Nota: I facet tornano SEMPRE una lista, anche se c'è un solo risultato
    private List<MetaStats> meta;

    @Data
    public static class CountEntry {
        @Field("_id") // Il group by in mongo mette la chiave in _id
        private String name; 
        private Integer count;
    }

    @Data
    public static class MetaStats {
        // Questi nomi devono combaciare con quelli nel $project del facet "meta"
        private Integer total_books_read;
        private ReviewData best_book;
        private ReviewData worst_book;
    }

    @Data
    public static class ReviewData {
        // Mappa i campi dell'oggetto review salvato nel DB
        private String _id;
        
        @Field("book") // Se nel DB il titolo è salvato come "book"
        private String title;
        
        // Se nel DB non hai l'autore nella review, questo sarà null
        // Se c'è, assicurati che il nome del campo combaci
        private String author; 
        
        private Integer rating;
    }
}
