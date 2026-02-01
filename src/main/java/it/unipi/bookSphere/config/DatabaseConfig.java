package it.unipi.bookSphere.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "it.unipi.bookSphere.repository.mongo")
@EnableNeo4jRepositories(basePackages = "it.unipi.bookSphere.repository.neo4j")
public class DatabaseConfig {

    @Value("${spring.data.mongodb.database:booksphere}")
    private String databaseName;

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, databaseName);
    }
}
