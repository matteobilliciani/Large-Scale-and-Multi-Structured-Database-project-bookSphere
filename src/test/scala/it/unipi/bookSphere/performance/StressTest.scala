package it.unipi.bookSphere.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Stress Test - Test di stress progressivo per BookSphere
 * 
 * Aumenta gradualmente il carico fino a trovare il breaking point del sistema.
 * Monitora quando il sistema inizia a degradare le performance.
 * 
 * Pattern: Incremento di 10 utenti ogni 30 secondi fino a 200 utenti
 */
class StressTest extends Simulation {

  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val initialUsers = Integer.getInteger("initialUsers", 5).intValue()
  val maxUsers = Integer.getInteger("maxUsers", 30).intValue()
  val stepDuration = Integer.getInteger("stepDuration", 10).intValue().seconds
  val incrementUsers = Integer.getInteger("incrementUsers", 5).intValue()

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Stress Test")

  val random = new scala.util.Random
  def randomString(length: Int): String = random.alphanumeric.take(length).mkString

  // Scenario che simula un utente reale con operazioni intensive
  val stressScenario = scenario("Heavy Load User Operations")
    .exec(http("Home Page - Browse Books")
      .get("/api/v1/books?page=0&size=20")
      .check(status.is(200)))
    .pause(500.milliseconds, 1.second)
    
    .exec(http("Browse Page 1")
      .get("/api/v1/books?page=1&size=20")
      .check(status.is(200)))
    .pause(300.milliseconds, 800.milliseconds)
    
    .exec(http("Browse Page 2")
      .get("/api/v1/books?page=2&size=20")
      .check(status.is(200)))
    .pause(300.milliseconds, 800.milliseconds)
    
    .exec(http("Browse Page 3")
      .get("/api/v1/books?page=3&size=20")
      .check(status.is(200)))
    .pause(300.milliseconds, 800.milliseconds)
    
    .exec(http("Browse Page 4")
      .get("/api/v1/books?page=4&size=20")
      .check(status.is(200)))
    .pause(300.milliseconds, 800.milliseconds)
    
    .exec(http("Browse Page 5")
      .get("/api/v1/books?page=5&size=20")
      .check(status.is(200)))
    .pause(1.second)
    
    .exec(http("Search Books - A")
      .get("/api/v1/books?title=a&page=0&size=20")
      .check(status.is(200)))
    .pause(500.milliseconds)
    
    .exec(http("Search Books - Book")
      .get("/api/v1/books?title=book&page=0&size=20")
      .check(status.is(200)))
    .pause(500.milliseconds)
    
    .exec(http("Search Authors - Smith")
      .get("/api/v1/authors?author_name=Smith&page=0&size=20")
      .check(status.in(200, 404)))
    .pause(500.milliseconds)
    
    .exec(http("Search Authors - John")
      .get("/api/v1/authors?author_name=John&page=0&size=20")
      .check(status.in(200, 404)))
    .pause(500.milliseconds)
    
    .exec(http("Get Authors List Page 0")
      .get("/api/v1/authors?page=0&size=20")
      .check(status.is(200)))
    .pause(500.milliseconds)
    
    .exec(http("Get Authors List Page 1")
      .get("/api/v1/authors?page=1&size=10")
      .check(status.is(200)))
    .pause(300.milliseconds)
    
    .exec(http("Get Authors List Page 2")
      .get("/api/v1/authors?page=2&size=10")
      .check(status.is(200)))
    .pause(300.milliseconds)

  // Stress Load Profile - Incremento graduale
  setUp(
    stressScenario.inject(
      incrementUsersPerSec(incrementUsers.toDouble / stepDuration.toSeconds)
        .times((maxUsers - initialUsers) / incrementUsers)
        .eachLevelLasting(stepDuration)
        .startingFrom(initialUsers.toDouble / stepDuration.toSeconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile3.lt(50000), // 95th percentile < 50s
      global.responseTime.percentile4.lt(80000), // 99th percentile < 80s
      global.successfulRequests.percent.gt(80)   // 80% success rate even under stress
    )
    .maxDuration(15.minutes)
}


