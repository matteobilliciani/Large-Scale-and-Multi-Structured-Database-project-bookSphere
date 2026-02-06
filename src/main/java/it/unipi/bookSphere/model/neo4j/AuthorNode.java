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
@Node("Author")
public class AuthorNode {
    
    @Id
    @Property("mongoId")
    @EqualsAndHashCode.Include
    private String mongoId;

    @Property("name")
    private String name;
}
