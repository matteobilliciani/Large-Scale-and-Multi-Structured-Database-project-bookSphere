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

  // Configuration
  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val users = Integer.getInteger("users", 50).intValue()
  val rampDuration = Integer.getInteger("rampDuration", 30).intValue().seconds
  val testDuration = Integer.getInteger("testDuration", 120).intValue().seconds

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

  // Scenario 1: Public API Access (no authentication)
  val publicScenario = scenario("Public API Access")
    .exec(http("Browse Books - Page 1")
      .get("/api/v1/books?page=0&size=20")
      .check(status.is(200))
      .check(jsonPath("$.content").exists)
      .check(responseTimeInMillis.lt(2000)))
    .pause(1, 3)
    .exec(http("Browse Books - Page 2")
      .get("/api/v1/books?page=1&size=20")
      .check(status.is(200)))
    .pause(1, 2)
    .exec(http("Search Book by Title")
      .get("/api/v1/books?title=Alice&page=0&size=20")
      .check(status.is(200)))
    .pause(1, 2)
    .exec(http("Get Authors List")
      .get("/api/v1/authors?page=0&size=10")
      .check(status.is(200)))

  // Load Profile Setup - Solo scenari pubblici per ora
  setUp(
    publicScenario.inject(
      rampUsers(users) during rampDuration,
      constantUsersPerSec(10) during testDuration
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(5000),
      global.responseTime.mean.lt(2000),
      global.successfulRequests.percent.gt(95)
    )
}


