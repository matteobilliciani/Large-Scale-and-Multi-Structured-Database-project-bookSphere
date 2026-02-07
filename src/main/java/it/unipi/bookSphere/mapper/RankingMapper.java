package it.unipi.bookSphere.mapper;

import it.unipi.bookSphere.dto.RankingDTO;
import it.unipi.bookSphere.repository.mongo.projections.RankingProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RankingMapper {

    // METODO DEFAULT: Gestisce il ciclo e passa il parametro 'year'
    default List<RankingDTO> toRankingDTOList(List<RankingProjection> projections, Integer year) {
        if (projections == null) {
            return new ArrayList<>();
        }
        return projections.stream()
                .map(proj -> toDTO(proj, year)) // Qui avviene la magia: passi l'anno a ogni riga
                .collect(Collectors.toList());
    }

    // MAPPING SINGOLO: MapStruct genererà l'implementazione di questo.
    // I campi con lo stesso nome (id, name, averageRating, etc.) vengono mappati AUTOMATICAMENTE.
    // Dobbiamo specificare solo 'year' perché viene dal secondo parametro.
    
    @Mapping(target = "year", source = "yearInput")
    RankingDTO toDTO(RankingProjection proj, Integer yearInput);
}
