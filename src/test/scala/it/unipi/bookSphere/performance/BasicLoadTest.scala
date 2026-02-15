package it.unipi.bookSphere.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Basic Load Test - Test di carico base per BookSphere
 * 
 * Simula 50 utenti concorrenti che eseguono operazioni comuni:
 * - Registrazione
 * - Login
 * - Navigazione catalogo libri
 * - Ricerca libri
 */
class BasicLoadTest extends Simulation {

  // Configuration - IMPROVED for better stress testing
  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val users = Integer.getInteger("users", 100).intValue()  // Increased from 20 to 100
  val rampDuration = Integer.getInteger("rampDuration", 20).intValue().seconds  // Increased from 10 to 20
  val testDuration = Integer.getInteger("testDuration", 60).intValue().seconds  // Increased from 30 to 60

  // HTTP Protocol Configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Performance Test")

  // Random data generators
  val random = new scala.util.Random
  def randomString(length: Int): String = random.alphanumeric.take(length).mkString
  def randomEmail(): String = s"user${randomString(8)}@test.com"

  // Feeders for test data
  val userFeeder = Iterator.continually(Map(
    "username" -> s"user_${randomString(10)}",
    "password" -> "Test123!@#",
    "email" -> randomEmail(),
    "name" -> s"TestUser${randomString(5)}",
    "surname" -> s"TestSurname${randomString(5)}"
  ))

  // Search terms for text search stress testing
  val searchTerms = List("Alice", "the", "Harry", "Lord", "Chronicles", "book", "story")
  val searchFeeder = Iterator.continually(Map(
    "searchTerm" -> searchTerms(random.nextInt(searchTerms.length))
  ))

  // Scenario 1: Public API Access (no authentication) - IMPROVED with more operations
  val publicScenario = scenario("Public API Access")
    .exec(http("Browse Books - Page 1")
      .get("/api/v1/books?page=0&size=50")  // Increased page size
      .check(status.is(200))
      .check(jsonPath("$.content").exists)
      .check(responseTimeInMillis.lt(3000)))
    .pause(500.milliseconds, 1.second)  // Reduced pause for more stress
    .exec(http("Browse Books - Page 2")
      .get("/api/v1/books?page=1&size=50")
      .check(status.is(200)))
    .pause(500.milliseconds)
    .feed(searchFeeder)
    .exec(http("Text Search Books")
      .get("/api/v1/books?title=${searchTerm}&page=0&size=30")  // Text search query
      .check(status.is(200)))
    .pause(500.milliseconds)
    .exec(http("Browse Books - Page 3")
      .get("/api/v1/books?page=2&size=50")
      .check(status.is(200)))
    .pause(500.milliseconds)
    .exec(http("Get Authors List")
      .get("/api/v1/authors?page=0&size=30")  // Increased page size
      .check(status.is(200)))
    .pause(500.milliseconds)
    .feed(searchFeeder)
    .exec(http("Text Search Authors")
      .get("/api/v1/authors?author_name=${searchTerm}&page=0&size=20")  // Text search query
      .check(status.is(200)))

  // Load Profile Setup - IMPROVED with more aggressive load
  setUp(
    publicScenario.inject(
      rampUsers(users) during rampDuration,
      constantUsersPerSec(25) during testDuration  // Increased from 10 to 25
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(8000),  // Relaxed from 5000 to 8000 for heavier load
      global.responseTime.mean.lt(3000),  // Relaxed from 2000 to 3000
      global.successfulRequests.percent.gt(90)  // Relaxed from 95 to 90
    )
}


