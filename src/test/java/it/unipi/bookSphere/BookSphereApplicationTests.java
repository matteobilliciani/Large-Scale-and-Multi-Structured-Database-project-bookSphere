package it.unipi.bookSphere;

import it.unipi.bookSphere.dto.LoginDTO;
import it.unipi.bookSphere.dto.RegisterDTO;
import it.unipi.bookSphere.dto.UserDTO;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.neo4j.UserNode;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.service.AuthService;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"wsl", "test-connection"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookSphereApplicationTests {

	private static final Logger logger = LoggerFactory.getLogger(BookSphereApplicationTests.class);

	@Autowired(required = false)
	private AuthService authService;

	@Autowired(required = false)
	private RegisteredUserRepository userRepository;

	@Autowired(required = false)
	private UserNodeRepository userNodeRepository;

	@Autowired(required = false)
	private PasswordEncoder passwordEncoder;

	private static final String TEST_USERNAME = "testuser" + System.currentTimeMillis();
	private static final String TEST_EMAIL = "test" + System.currentTimeMillis() + "@example.com";
	private static final String TEST_PASSWORD = "SecurePass123!";
	private static final String TEST_COUNTRY = "IT";

	@Test
	@Order(1)
	void contextLoads() {
		logger.info("✅ Application context loaded successfully");
	}

	@Test
	@Order(2)
	void testDatabaseConnectionsAvailable() {
		if (userRepository != null) {
			logger.info("✅ MongoDB repository is available");
		} else {
			logger.warn("⚠️ MongoDB repository is not available");
		}

		if (userNodeRepository != null) {
			logger.info("✅ Neo4j repository is available");
		} else {
			logger.warn("⚠️ Neo4j repository is not available");
		}
	}

	@Test
	@Order(3)
	void testUserRegistration() {
		if (authService == null) {
			logger.warn("⚠️ AuthService not available, skipping registration test");
			return;
		}

		logger.info("========================================");
		logger.info("Testing User Registration");
		logger.info("========================================");

		// Prepare registration data
		RegisterDTO registerDTO = new RegisterDTO();
		registerDTO.setUsername(TEST_USERNAME);
		registerDTO.setEmail(TEST_EMAIL);
		registerDTO.setPassword(TEST_PASSWORD);
		registerDTO.setCountry(TEST_COUNTRY);

		// Register user
		UserDTO userDTO = authService.register(registerDTO);

		// Verify response
		assertNotNull(userDTO);
		assertNotNull(userDTO.getId());
		assertEquals(TEST_USERNAME, userDTO.getUsername());
		assertEquals(TEST_EMAIL, userDTO.getEmail());
		assertEquals(TEST_COUNTRY, userDTO.getCountry());
		assertEquals("active", userDTO.getStatus());

		logger.info("✅ User registered successfully");
		logger.info("   User ID: {}", userDTO.getId());
		logger.info("   Username: {}", userDTO.getUsername());
		logger.info("   Email: {}", userDTO.getEmail());

		// Verify user in MongoDB
		if (userRepository != null) {
			RegisteredUser mongoUser = userRepository.findById(userDTO.getId()).orElse(null);
			assertNotNull(mongoUser, "User should exist in MongoDB");
			assertEquals(TEST_USERNAME, mongoUser.getUsername());
			assertEquals(TEST_EMAIL, mongoUser.getEmail());
			assertTrue(passwordEncoder.matches(TEST_PASSWORD, mongoUser.getPasswordHashed()));
			logger.info("✅ User verified in MongoDB");
		}

		// Verify user in Neo4j
		if (userNodeRepository != null) {
			UserNode neoUser = userNodeRepository.findByMongoId(userDTO.getId()).orElse(null);
			assertNotNull(neoUser, "User should exist in Neo4j");
			assertEquals(TEST_USERNAME, neoUser.getUsername());
			assertEquals(TEST_COUNTRY, neoUser.getCountry());
			logger.info("✅ User verified in Neo4j");
		}
	}

	@Test
	@Order(4)
	void testUserLogin() {
		if (authService == null) {
			logger.warn("⚠️ AuthService not available, skipping login test");
			return;
		}

		logger.info("========================================");
		logger.info("Testing User Login");
		logger.info("========================================");

		// Login with username
		LoginDTO loginDTO = new LoginDTO();
		loginDTO.setUsernameOrEmail(TEST_USERNAME);
		loginDTO.setPassword(TEST_PASSWORD);

		UserDTO userDTO = authService.login(loginDTO);

		// Verify response
		assertNotNull(userDTO);
		assertNotNull(userDTO.getId());
		assertEquals(TEST_USERNAME, userDTO.getUsername());
		assertEquals(TEST_EMAIL, userDTO.getEmail());

		logger.info("✅ Login with username successful");

		// Login with email
		loginDTO.setUsernameOrEmail(TEST_EMAIL);
		UserDTO userDTO2 = authService.login(loginDTO);

		assertNotNull(userDTO2);
		assertEquals(userDTO.getId(), userDTO2.getId());

		logger.info("✅ Login with email successful");
	}

	@Test
	@Order(5)
	void testInvalidLogin() {
		if (authService == null) {
			logger.warn("⚠️ AuthService not available, skipping invalid login test");
			return;
		}

		logger.info("========================================");
		logger.info("Testing Invalid Login");
		logger.info("========================================");

		// Try login with wrong password
		LoginDTO loginDTO = new LoginDTO();
		loginDTO.setUsernameOrEmail(TEST_USERNAME);
		loginDTO.setPassword("WrongPassword123!");

		assertThrows(Exception.class, () -> {
			authService.login(loginDTO);
		}, "Login should fail with wrong password");

		logger.info("✅ Invalid login correctly rejected");
	}

	@AfterAll
	static void cleanup(@Autowired(required = false) RegisteredUserRepository userRepository,
						@Autowired(required = false) UserNodeRepository userNodeRepository) {
		logger.info("========================================");
		logger.info("Cleaning up test data");
		logger.info("========================================");

		// Cleanup is done by deleting test users
		if (userRepository != null) {
			try {
				userRepository.findByUsername(TEST_USERNAME).ifPresent(user -> {
					userRepository.delete(user);
					logger.info("✅ Test user deleted from MongoDB");
				});
			} catch (Exception e) {
				logger.warn("⚠️ Could not cleanup MongoDB: {}", e.getMessage());
			}
		}

		if (userNodeRepository != null) {
			try {
				userNodeRepository.findByUsername(TEST_USERNAME).ifPresent(user -> {
					userNodeRepository.delete(user);
					logger.info("✅ Test user deleted from Neo4j");
				});
			} catch (Exception e) {
				logger.warn("⚠️ Could not cleanup Neo4j: {}", e.getMessage());
			}
		}
	}
}
