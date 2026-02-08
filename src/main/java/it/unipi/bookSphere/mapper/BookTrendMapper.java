package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.BookTrendDTO;
import it.unipi.bookSphere.repository.mongo.projections.BookTrendProjection;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BookTrendMapper {

    // METODO DEFAULT: Gestisce il ciclo e la posizione
    default List<BookTrendDTO> toDtoList(List<BookTrendProjection> projections) {
        if (projections == null) return new ArrayList<>();

        List<BookTrendDTO> list = new ArrayList<>();
        int rank = 1;

        for (BookTrendProjection proj : projections) {
            list.add(toDTO(proj, rank));
            rank++;
        }
        return list;
    }

    // MAPPING SINGOLO (Custom implementation per la descrizione stringa e null safety)
    default BookTrendDTO toDTO(BookTrendProjection proj, Integer position) {
        if (proj == null) return null;

        // Null Safety
        double startR = proj.getStartRating() != null ? proj.getStartRating() : 0.0;
        double endR = proj.getEndRating() != null ? proj.getEndRating() : 0.0;
        double delta = proj.getRatingDelta() != null ? proj.getRatingDelta() : 0.0;
        
        // Arrotondamento
        startR = Math.round(startR * 100.0) / 100.0;
        endR = Math.round(endR * 100.0) / 100.0;
        delta = Math.round(delta * 100.0) / 100.0;

        String trendDesc = String.format("Started at %.1f -> Ended at %.1f (Change: %+.2f)", 
                                         startR, endR, delta);

        return new BookTrendDTO(
            position,
            proj.getId(),
            proj.getTitle(),
            proj.getAuthor(),
            proj.getStartYear(),
            proj.getEndYear(),
            startR,
            endR,
            delta,
            trendDesc
        );
    }
}
