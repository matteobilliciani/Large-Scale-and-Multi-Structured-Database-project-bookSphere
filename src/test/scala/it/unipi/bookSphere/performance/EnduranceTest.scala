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
  val constantUsers = Integer.getInteger("constantUsers", 30).intValue()
  val testDuration = Integer.getInteger("testDuration", 3).intValue().minutes

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Endurance Test")

  val random = new scala.util.Random

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
          
          25.0 -> exec(http("Browse Random Page")
            .get(s"/api/books?page=${random.nextInt(10)}&size=20")
            .check(status.is(200))),
          
          25.0 -> exec(http("Filter by Category")
            .get("/api/v1/books/filter?category=Science Fiction&page=0&size=20")
            .check(status.is(200))),
          
          20.0 -> exec(http("Search Authors")
            .get("/api/v1/authors?author_name=Smith&page=0&size=20")
            .check(status.in(200, 404)))
        )
        .pause(3.seconds, 8.seconds)
        
        .exec(http("Get Book Details")
          .get("/api/v1/books/679cb334b477993c5cdc8b3d")
          .check(status.is(200)))
        .pause(5.seconds, 10.seconds)
    }

  setUp(
    enduranceScenario.inject(
      rampUsers(constantUsers) during 5.minutes,        // Ramp-up iniziale
      constantUsersPerSec(constantUsers / 60.0) during testDuration  // Carico costante
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.mean.lt(1500),                // Media < 1.5s per tutta la durata
      global.responseTime.percentile3.lt(3000),         // 95th percentile < 3s
      global.successfulRequests.percent.gt(99),         // > 99% successo
      global.responseTime.max.lt(10000)                 // Max < 10s anche dopo 2 ore
    )
    .throttle(
      reachRps(constantUsers * 2) in 5.minutes,         // Limitiamo le RPS per evitare sovraccarico
      holdFor(testDuration)
    )
}


