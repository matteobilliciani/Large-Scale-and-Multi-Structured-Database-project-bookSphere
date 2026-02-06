package it.unipi.bookSphere.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Node("Review")
public class ReviewNode {
    
    @Id
    @Property("mongoId")
    @EqualsAndHashCode.Include
    private String mongoId;

    @Property("rating")
    private Integer rating;

    @Property("createdAt")
    private LocalDateTime createdAt;

    // Relationships

    @Relationship(type = "POSTED", direction = Relationship.Direction.INCOMING)
    private UserNode author;

    @Relationship(type = "REFER_TO", direction = Relationship.Direction.OUTGOING)
    private BookNode book;
}
