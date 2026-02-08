package it.unipi.bookSphere.config;

import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@Profile("test-connection")
public class DatabaseConnectionTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionTest.class);

    @Bean
    CommandLineRunner testDatabaseConnections(
            MongoTemplate mongoTemplate,
            Driver neo4jDriver) {
        return args -> {
            logger.info("========================================");
            logger.info("Testing Database Connections");
            logger.info("========================================");

            // Test MongoDB
            try {
                logger.info("\n📊 Testing MongoDB connection...");
                String dbName = mongoTemplate.getDb().getName();
                var collections = mongoTemplate.getCollectionNames();
                logger.info("✅ MongoDB connected successfully!");
                logger.info("   Database: {}", dbName);
                logger.info("   Collections: {}", collections);
            } catch (Exception e) {
                logger.error("❌ MongoDB connection failed: {}", e.getMessage());
                e.printStackTrace();
            }

            // Test Neo4j
            try {
                logger.info("\n🔗 Testing Neo4j connection...");
                neo4jDriver.verifyConnectivity();
                
                // Execute a simple query
                try (var session = neo4jDriver.session()) {
                    var result = session.run("RETURN 'Connection successful!' AS message");
                    var message = result.single().get("message").asString();
                    logger.info("✅ Neo4j connected successfully!");
                    logger.info("   Message: {}", message);
                    
                    // Count nodes
                    var countResult = session.run("MATCH (n) RETURN count(n) AS count");
                    var count = countResult.single().get("count").asLong();
                    logger.info("   Total nodes: {}", count);
                }
            } catch (Exception e) {
                logger.error("❌ Neo4j connection failed: {}", e.getMessage());
                e.printStackTrace();
            }

            logger.info("\n========================================");
            logger.info("Connection test completed!");
            logger.info("========================================\n");
        };
    }
}
