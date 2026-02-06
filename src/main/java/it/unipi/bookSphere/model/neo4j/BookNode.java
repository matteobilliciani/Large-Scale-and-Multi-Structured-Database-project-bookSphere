package it.unipi.bookSphere.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Node("Book")
public class BookNode {
    
    @Id
    @Property("mongoId")
    @EqualsAndHashCode.Include
    private String mongoId;

    @Property("title")
    private String title;

    @Property("year")
    private Integer year;
}
