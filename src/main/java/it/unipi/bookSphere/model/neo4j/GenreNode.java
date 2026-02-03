package it.unipi.bookSphere.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("Genre")
public class GenreNode {
    
    @Id
    @Property("name")
    private String name;

    // Relationships

    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.INCOMING)
    private Set<BookNode> books = new HashSet<>();

    @Relationship(type = "LIKES", direction = Relationship.Direction.INCOMING)
    private Set<UserNode> likedByUsers = new HashSet<>();
}
