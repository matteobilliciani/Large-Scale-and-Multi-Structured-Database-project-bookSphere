package it.unipi.bookSphere.config;

import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

/**
 * Creates the default admin user in MongoDB on application startup if it does not exist.
 * Admin users are identified by status = "ADMIN".
 *
 * Credentials can be overridden via application properties:
 *   admin.username   (default: admin)
 *   admin.email      (default: admin@booksphere.it)
 *   admin.password   (default: Admin1234!)
 *   admin.country    (default: IT)
 */
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final RegisteredUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.email:admin@booksphere.it}")
    private String adminEmail;

    @Value("${admin.password:Admin1234!}")
    private String adminPassword;

    @Value("${admin.country:IT}")
    private String adminCountry;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(adminUsername)) {
            logger.info("Admin user '{}' already exists – skipping creation.", adminUsername);
            return;
        }

        RegisteredUser admin = new RegisteredUser();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPasswordHashed(passwordEncoder.encode(adminPassword));
        admin.setCountry(adminCountry);
        admin.setJoinedAt(Instant.now());
        admin.setStatus("ADMIN");
        admin.setBookshelf(new ArrayList<>());
        admin.setReviewsYear(new ArrayList<>());
        admin.setReviews(new ArrayList<>());

        RegisteredUser saved = userRepository.save(admin);
        logger.info("Default admin user created with id={} username='{}'. CHANGE THE DEFAULT PASSWORD IN PRODUCTION!",
                saved.getId(), saved.getUsername());
    }
}
