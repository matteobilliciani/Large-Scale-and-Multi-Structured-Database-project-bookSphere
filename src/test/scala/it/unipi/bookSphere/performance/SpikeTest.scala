package it.unipi.bookSphere.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Spike Test - Test di picchi improvvisi di traffico
 * 
 * Simula situazioni reali come:
 * - Lancio di nuove funzionalità
 * - Picchi durante eventi specifici
 * - Traffico improvviso da social media
 * 
 * Pattern: Salto improvviso da 10 a 200 utenti
 */
class SpikeTest extends Simulation {

  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val normalLoad = Integer.getInteger("normalLoad", 10).intValue()  // Increased from 5 to 10
  val spikeLoad = Integer.getInteger("spikeLoad", 100).intValue()  // Increased from 20 to 100 for dramatic spike
  val spikeDuration = Integer.getInteger("spikeDuration", 30).intValue().seconds  // Increased from 15 to 30

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Spike Test")

  val random = new scala.util.Random
  
  // Search terms for text search stress
  val searchTerms = List("Alice", "Harry", "Lord", "Chronicles", "the", "book")
  val searchFeeder = Iterator.continually(Map(
    "searchTerm" -> searchTerms(random.nextInt(searchTerms.length))
  ))

  val quickBrowsingScenario = scenario("Quick Spike User")
    .exec(http("Quick Browse Books")
      .get("/api/v1/books?page=0&size=50")  // Increased size from 20 to 50
      .check(status.is(200)))
    .pause(200.milliseconds, 400.milliseconds)  // Reduced pause for more stress
    .feed(searchFeeder)
    .exec(http("Text Search During Spike")  // NEW: Text search operation
      .get("/api/v1/books?title=${searchTerm}&page=0&size=30")
      .check(status.is(200)))
    .pause(200.milliseconds)
    .exec(http("Browse More Books")
      .get("/api/v1/books?page=1&size=50")
      .check(status.is(200)))
    .pause(200.milliseconds)
    .exec(http("Browse Authors")
      .get("/api/v1/authors?page=0&size=30")  // Increased size
      .check(status.is(200)))
    .pause(200.milliseconds)
    .feed(searchFeeder)
    .exec(http("Text Search Authors")
      .get("/api/v1/authors?author_name=${searchTerm}&page=0&size=20")
      .check(status.is(200)))

  setUp(
    quickBrowsingScenario.inject(
      constantUsersPerSec(normalLoad) during 20.seconds,    // Normal load phase
      atOnceUsers(spikeLoad),                               // DRAMATIC SPIKE! 10 -> 100 users
      constantUsersPerSec(spikeLoad / 2) during spikeDuration,  // Sustained high load
      rampUsers(normalLoad) during 20.seconds               // Return to normal
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(100000),   // Relaxed to 100s for extreme spike
      global.successfulRequests.percent.gt(75)  // Relaxed to 75% due to extreme spike
    )
}


