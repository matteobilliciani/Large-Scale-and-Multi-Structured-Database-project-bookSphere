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
@Node("User")
public class UserNode {
    
    @Id
    @Property("mongoId")
    private String mongoId;

    @Property("username")
    private String username;

    @Property("country")
    private String country;

    // Relationships
    
    @Relationship(type = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
    private Set<FollowsRelationship> following = new HashSet<>();

    @Relationship(type = "LIKES", direction = Relationship.Direction.OUTGOING)
    private Set<LikesReviewRelationship> likedReviews = new HashSet<>();

    @Relationship(type = "LIKES", direction = Relationship.Direction.OUTGOING)
    private Set<LikesBookRelationship> likedBooks = new HashSet<>();

    @Relationship(type = "LIKES", direction = Relationship.Direction.OUTGOING)
    private Set<GenreNode> likedGenres = new HashSet<>();

    @Relationship(type = "LIKES", direction = Relationship.Direction.OUTGOING)
    private Set<AuthorNode> likedAuthors = new HashSet<>();

    @Relationship(type = "POSTED", direction = Relationship.Direction.OUTGOING)
    private Set<ReviewNode> postedReviews = new HashSet<>();

    // Relationship POJOs

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowsRelationship {
        private UserNode followedUser;
        
        @Property("since")
        private LocalDateTime since;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LikesReviewRelationship {
        private ReviewNode review;
        
        @Property("timestamp")
        private LocalDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LikesBookRelationship {
        private BookNode book;
        
        @Property("timestamp")
        private LocalDateTime timestamp;
    }
}
