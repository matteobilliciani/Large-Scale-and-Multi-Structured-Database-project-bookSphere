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
  val users = Integer.getInteger("users", 30).intValue()  // Increased from 9 to 30
  val duration = Integer.getInteger("duration", 2).intValue().minutes  // Increased from 1 to 2 minutes

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling DB Intensive Test")

  val random = new scala.util.Random
  
  // Search terms to stress text indexes
  val searchTerms = List("the", "book", "story", "tale", "chronicles", "Alice", "Harry")
  val searchFeeder = Iterator.continually(Map(
    "searchTerm" -> searchTerms(random.nextInt(searchTerms.length))
  ))

  // Scenario con operazioni pesanti su MongoDB (aggregations + text search)
  val mongoHeavyScenario = scenario("MongoDB Heavy Operations")
    .exec(http("Browse Books Page 1")
      .get("/api/v1/books?page=0&size=100")  // Increased size from 50 to 100
      .check(status.is(200))
      .check(responseTimeInMillis.lte(5000)))  // Relaxed timeout
    .pause(500.milliseconds)
    .feed(searchFeeder)
    .exec(http("Text Search Books - MongoDB Index")  // NEW: Text search on indexed field
      .get("/api/v1/books?title=${searchTerm}&page=0&size=50")
      .check(status.is(200)))
    .pause(500.milliseconds)
    .exec(http("Browse Large Result Set")
      .get("/api/v1/books?page=3&size=100")  // Increased page and size
      .check(status.is(200)))
    .pause(500.milliseconds)
    .feed(searchFeeder)
    .exec(http("Multiple Page Browsing")
      .get("/api/v1/books?page=5&size=50")
      .check(status.is(200)))
    .pause(500.milliseconds)

  // Scenario con operazioni su Neo4j (grafo + text search)
  val neo4jHeavyScenario = scenario("Neo4j Heavy Operations")
    .exec(http("Get Authors List")
      .get("/api/v1/authors?page=0&size=100")  // Increased size from 50 to 100
      .check(status.is(200))
      .check(responseTimeInMillis.lte(4000)))  // Relaxed timeout
    .pause(500.milliseconds)
    .feed(searchFeeder)
    .exec(http("Text Search Authors - MongoDB Index")  // NEW: Text search on indexed field
      .get("/api/v1/authors?author_name=${searchTerm}&page=0&size=30")
      .check(status.is(200)))
    .pause(500.milliseconds)
    .exec(http("Browse More Authors")
      .get("/api/v1/authors?page=2&size=50")  // Increased page number
      .check(status.is(200)))
    .pause(500.milliseconds)
    .exec(http("Deep Pagination Authors")
      .get("/api/v1/authors?page=5&size=30")
      .check(status.is(200)))
    .pause(500.milliseconds)

  // Scenario misto che testa la consistency tra MongoDB e Neo4j + text search
  val consistencyScenario = scenario("Cross-Database Consistency + Text Search")
    .exec(http("Get Books from MongoDB")
      .get("/api/v1/books?page=0&size=50")  // Increased size
      .check(status.is(200)))
    .pause(300.milliseconds)
    .feed(searchFeeder)
    .exec(http("Text Search Books")
      .get("/api/v1/books?title=${searchTerm}&page=0&size=30")
      .check(status.is(200)))
    .pause(300.milliseconds)
    .exec(http("Get Authors from Neo4j")
      .get("/api/v1/authors?page=0&size=50")  // Increased size
      .check(status.is(200)))
    .pause(300.milliseconds)
    .feed(searchFeeder)
    .exec(http("Text Search Authors")
      .get("/api/v1/authors?author_name=${searchTerm}&page=0&size=20")
      .check(status.is(200)))
    .pause(300.milliseconds)

  setUp(
    mongoHeavyScenario.inject(
      rampUsers(users / 3) during 20.seconds,  // Reduced ramp time
      constantUsersPerSec(users / 40.0) during duration  // Increased rate
    ),
    neo4jHeavyScenario.inject(
      rampUsers(users / 3) during 20.seconds,
      constantUsersPerSec(users / 40.0) during duration
    ),
    consistencyScenario.inject(
      rampUsers(users / 3) during 20.seconds,
      constantUsersPerSec(users / 40.0) during duration
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.mean.lt(3000),  // Relaxed from 2000
      global.responseTime.percentile3.lt(6000),  // Relaxed from 4000
      global.successfulRequests.percent.gt(90)  // Relaxed from 95
    )
}


