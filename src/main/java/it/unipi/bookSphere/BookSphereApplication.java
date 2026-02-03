package it.unipi.bookSphere;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class BookSphereApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookSphereApplication.class, args);
	}

}
