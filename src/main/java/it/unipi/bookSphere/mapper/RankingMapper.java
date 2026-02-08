package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.RankingDTO;
import it.unipi.bookSphere.repository.mongo.projections.RankingProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface RankingMapper {

    // METODO DEFAULT AGGIORNATO: Gestisce il ciclo e assegna la posizione
    default List<RankingDTO> toRankingDTOList(List<RankingProjection> projections, Integer year) {
        if (projections == null) {
            return new ArrayList<>();
        }

        List<RankingDTO> list = new ArrayList<>();
        int rank = 1; // Contatore posizione

        for (RankingProjection proj : projections) {
            // Chiamiamo il metodo di mapping singolo passando anche il rank corrente
            list.add(toDTO(proj, year, rank));
            rank++; // Incrementiamo per il prossimo
        }

        return list;
    }

    // MAPPING SINGOLO AGGIORNATO
    // Ora accetta 3 parametri: la proiezione, l'anno e la posizione.
    // MapStruct mapperà automaticamente i campi con lo stesso nome dalla proiezione.
    
    @Mapping(target = "year", source = "yearInput")
    @Mapping(target = "position", source = "positionInput") // Mappa il parametro rank
    RankingDTO toDTO(RankingProjection proj, Integer yearInput, Integer positionInput);
}