package it.unipi.bookSphere.config;

import com.mongodb.client.MongoClient;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableMongoRepositories(basePackages = "it.unipi.bookSphere.repository.mongo")
@EnableNeo4jRepositories(basePackages = "it.unipi.bookSphere.repository.neo4j")
@EnableAsync
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.data.mongodb.database:booksphere}")
    private String databaseName;

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, databaseName);
    }

    /**
     * Initialize MongoDB text indexes for optimized search
     */
    @Bean
    public CommandLineRunner initializeTextIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            try {
                logger.info("Initializing text indexes for MongoDB...");
                
                // Create text index on Book.title
                TextIndexDefinition bookTitleIndex = TextIndexDefinition.builder()
                        .onField("title")
                        .build();
                
                mongoTemplate.indexOps(BookDocument.class).ensureIndex(bookTitleIndex);
                logger.info("Text index created on Book.title");
                
                // Create multi-key index on Book.genres for efficient genre-based queries
                mongoTemplate.indexOps(BookDocument.class).ensureIndex(
                    new org.springframework.data.mongodb.core.index.Index().on("genres", 
                        org.springframework.data.domain.Sort.Direction.ASC)
                );
                logger.info("Multi-key index created on Book.genres");
                
                // Create text index on Author.name
                TextIndexDefinition authorNameIndex = TextIndexDefinition.builder()
                        .onField("name")
                        .build();
                
                mongoTemplate.indexOps(AuthorDocument.class).ensureIndex(authorNameIndex);
                logger.info("Text index created on Author.name");
                
                logger.info("Text indexes initialization completed successfully");
            } catch (Exception e) {
                logger.error("Error initializing text indexes: {}", e.getMessage());
                // Don't fail the application, just log the error
            }
        };
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
