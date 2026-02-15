package it.unipi.bookSphere;

import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Custom annotation to centralize test profile configuration.
 * 
 * CHANGE ONLY THIS LINE to use a different profile for ALL tests:
 * @ActiveProfiles("clusterWSL")  <-- Change "clusterWSL" to "local", "cluster", "wsl", etc.
 * 
 * Usage: Replace @ActiveProfiles("...") with @TestProfile in all test classes
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ActiveProfiles("")  // <-- CHANGE THIS SINGLE LINE to switch profile for all tests
public @interface TestProfile {
}
