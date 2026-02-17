package it.unipi.bookSphere.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Endurance Test (Soak Test) - Test di resistenza prolungato
 * 
 * Verifica la stabilità del sistema sotto carico costante per periodi lunghi.
 * Identifica memory leaks, degradazione delle performance, problemi di risorse.
 * 
 * Pattern: 50 utenti costanti per 2 ore
 */
class EnduranceTest extends Simulation {

  val baseUrl = System.getProperty("baseUrl", "http://localhost:8080")
  val constantUsers = Integer.getInteger("constantUsers", 40).intValue()  // 40 users for stable endurance test
  val testDuration = Integer.getInteger("testDuration", 2).intValue().minutes

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Endurance Test")

  val random = new scala.util.Random
  val pageFeeder = Iterator.continually(Map(
    "randomPage" -> random.nextInt(5)
  ))

  // Scenario realistico di un utente che usa l'app ripetutamente
  val enduranceScenario = scenario("Long Running User Session")
    .during(testDuration) {
      exec(http("Browse Books Homepage")
        .get("/api/v1/books?page=0&size=20")
        .check(status.is(200)))
        .pause(2.seconds, 5.seconds)
        
        .randomSwitch(
          30.0 -> exec(http("Search by Title")
            .get("/api/v1/books?title=The&page=0&size=20")
            .check(status.is(200))),
          
          35.0 -> feed(pageFeeder) 
            .exec(http("Browse Random Page")
            .get(s"/api/v1/books?page=#{randomPage}&size=20")
            .check(status.is(200))),
          
          35.0 -> exec(http("Search Authors")
            .get("/api/v1/authors?author_name=Smith&page=0&size=20")
            .check(status.in(200, 404)))
        )
        .pause(3.seconds, 8.seconds)
        
        .exec(http("Browse More Books")
          .get("/api/v1/books?page=1&size=20")
          .check(status.is(200)))
        .pause(5.seconds, 10.seconds)
    }

  setUp(
    enduranceScenario.inject(
      rampUsers(constantUsers) during 1.minute,        // Ramp-up iniziale
      constantUsersPerSec(constantUsers / 180.0) during testDuration  // Very gentle constant load
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.mean.lt(500),              // Mean < 500ms sustained
      global.responseTime.percentile3.lt(1200),      // p95 < 1.2s
      global.successfulRequests.percent.gt(95),      // 95%+ success
      global.responseTime.max.lt(2500)               // Max < 2.5s after prolonged test
    )
    .throttle(
      reachRps(constantUsers) in 1.minute,         // Gentle RPS for stability (1 RPS per user)
      holdFor(testDuration)
    )
}


