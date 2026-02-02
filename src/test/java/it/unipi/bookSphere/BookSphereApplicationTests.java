package it.unipi.bookSphere;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"wsl", "test-connection"})
class BookSphereApplicationTests {

	@Test
	void contextLoads() {
	}

}
