package it.unipi.bookSphere.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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

    // Relationships

    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
    private Set<GenreNode> genres = new HashSet<>();

    @Relationship(type = "WROTE", direction = Relationship.Direction.INCOMING)
    private AuthorNode author;

    @Relationship(type = "REFER_TO", direction = Relationship.Direction.INCOMING)
    private Set<ReviewNode> reviews = new HashSet<>();

    @Relationship(type = "LIKES", direction = Relationship.Direction.INCOMING)
    private Set<UserNode> likedByUsers = new HashSet<>();
}
