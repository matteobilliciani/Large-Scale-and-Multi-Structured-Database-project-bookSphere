package it.unipi.bookSphere.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Node("Review")
public class ReviewNode {
    
    @Id
    @Property("mongoId")
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

    @Relationship(type = "LIKES", direction = Relationship.Direction.INCOMING)
    private Set<UserNode> likedByUsers = new HashSet<>();
}
