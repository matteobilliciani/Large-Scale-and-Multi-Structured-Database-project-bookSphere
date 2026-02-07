package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.WrappedDTO;
import it.unipi.bookSphere.repository.mongo.projections.WrappedAggregationResult;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface WrappedMapper {

    // --- MAPPING PRINCIPALE ---
    
    @Mapping(target = "year", source = "year")
    @Mapping(target = "topAuthors", source = "result.top_authors")
    @Mapping(target = "topGenres", source = "result.top_genres")
    // Ignoriamo questi perché li calcoliamo a mano nell'AfterMapping estraendoli da "meta"
    @Mapping(target = "totalBooksRead", ignore = true)
    @Mapping(target = "bestBook", ignore = true)
    @Mapping(target = "worstBook", ignore = true)
    WrappedDTO toDTO(WrappedAggregationResult result, Integer year);


    // --- SOTTO-MAPPING (MapStruct li usa automaticamente per le liste) ---

    // Da CountEntry (Mongo) -> AuthorFrequencyDTO (Tuo DTO)
    WrappedDTO.AuthorFrequencyDTO mapAuthor(WrappedAggregationResult.CountEntry entry);

    // Da CountEntry (Mongo) -> GenreFrequencyDTO (Tuo DTO)
    WrappedDTO.GenreFrequencyDTO mapGenre(WrappedAggregationResult.CountEntry entry);

    // Da ReviewData (Mongo) -> BookSummaryWithRatingDTO (Tuo DTO)
    @Mapping(target = "id", source = "_id")
    @Mapping(target = "authorName", source = "author", defaultValue = "Unknown Author") // Gestione null safe
    WrappedDTO.BookSummaryWithRatingDTO mapBookSummary(WrappedAggregationResult.ReviewData data);


    // --- LOGICA CUSTOM PER ESTRARRE I DATI DA META ---

    @AfterMapping
    default void extractMetaStats(WrappedAggregationResult result, @MappingTarget WrappedDTO dto) {
        // Controllo difensivo: il facet "meta" potrebbe essere vuoto se qualcosa va storto
        if (result.getMeta() != null && !result.getMeta().isEmpty()) {
            
            WrappedAggregationResult.MetaStats stats = result.getMeta().get(0);

            dto.setTotalBooksRead(stats.getTotal_books_read() != null ? stats.getTotal_books_read() : 0);
            
            // Usiamo i metodi di mapping definiti sopra (o li chiamiamo manualmente se MapStruct non li vede qui)
            // Nota: MapStruct genera l'implementazione, quindi qui dobbiamo invocare logica manuale o delegare
            
            if (stats.getBest_book() != null) {
                // Conversione manuale veloce o estrazione metodo
                dto.setBestBook(toBookSummary(stats.getBest_book()));
            }
            
            if (stats.getWorst_book() != null) {
                dto.setWorstBook(toBookSummary(stats.getWorst_book()));
            }
        } else {
            dto.setTotalBooksRead(0);
        }
    }

    // Helper method per l'AfterMapping
    default WrappedDTO.BookSummaryWithRatingDTO toBookSummary(WrappedAggregationResult.ReviewData data) {
        if (data == null) return null;
        return new WrappedDTO.BookSummaryWithRatingDTO(
            data.get_id(),
            data.getTitle(),
            data.getAuthor() != null ? data.getAuthor() : "Unknown Author",
            data.getRating()
        );
    }
}
