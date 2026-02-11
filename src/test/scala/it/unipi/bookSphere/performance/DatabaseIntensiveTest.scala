package it.unipi.bookSphere.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Database Intensive Test - Test focalizzato su operazioni pesanti su DB
 * 
 * Testa specificamente le performance di:
 * - Aggregation pipelines MongoDB
 * - Query complesse Neo4j
 * - Consistency tra i due database
 * 
 * Pattern: Operazioni intensive su entrambi i database
 */
class DatabaseIntensiveTest extends Simulation {

  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val users = Integer.getInteger("users", 20).intValue()
  val duration = Integer.getInteger("duration", 2).intValue().minutes

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling DB Intensive Test")

  val random = new scala.util.Random

  // Scenario con operazioni pesanti su MongoDB (aggregations)
  val mongoHeavyScenario = scenario("MongoDB Heavy Operations")
    .exec(http("Browse Books Page 1")
      .get("/api/v1/books?page=0&size=50")
      .check(status.is(200))
      .check(responseTimeInMillis.lte(3000)))
    .pause(1.second)
    
    .exec(http("Search Books - The")
      .get("/api/v1/books?title=The&page=0&size=20")
      .check(status.in(200, 404)))
    .pause(1.second)
    
    .exec(http("Browse Large Result Set")
      .get("/api/v1/books?page=2&size=50")
      .check(status.is(200)))
    .pause(2.seconds)

  // Scenario con operazioni su Neo4j (grafo)
  val neo4jHeavyScenario = scenario("Neo4j Heavy Operations")
    .exec(http("Get Authors List")
      .get("/api/v1/authors?page=0&size=50")
      .check(status.is(200))
      .check(responseTimeInMillis.lte(2000)))
    .pause(1.second)
    
    .exec(http("Search Authors by Name")
      .get("/api/v1/authors?author_name=Smith&page=0&size=20")
      .check(status.in(200, 404)))
    .pause(1.second)
    
    .exec(http("Browse More Authors")
      .get("/api/v1/authors?page=1&size=30")
      .check(status.is(200)))
    .pause(2.seconds)

  // Scenario misto che testa la consistency tra MongoDB e Neo4j
  val consistencyScenario = scenario("Cross-Database Consistency")
    .exec(http("Get Books from MongoDB")
      .get("/api/v1/books?page=0&size=20")
      .check(status.is(200)))
    .pause(500.milliseconds)
    
    .exec(http("Search Books by Title")
      .get("/api/v1/books?title=book&page=0&size=20")
      .check(status.is(200)))
    .pause(1.second)
    
    .exec(http("Get Authors from Neo4j")
      .get("/api/v1/authors?page=0&size=20")
      .check(status.is(200)))

  setUp(
    mongoHeavyScenario.inject(
      rampUsers(users / 3) during 2.minutes,
      constantUsersPerSec(users / 60.0) during duration
    ),
    neo4jHeavyScenario.inject(
      rampUsers(users / 3) during 2.minutes,
      constantUsersPerSec(users / 60.0) during duration
    ),
    consistencyScenario.inject(
      rampUsers(users / 3) during 2.minutes,
      constantUsersPerSec(users / 60.0) during duration
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.mean.lt(2000),
      global.responseTime.percentile3.lt(4000),
      global.successfulRequests.percent.gt(95)
    )
}


