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
  val normalLoad = Integer.getInteger("normalLoad", 10).intValue()
  val spikeLoad = Integer.getInteger("spikeLoad", 50).intValue()
  val spikeDuration = Integer.getInteger("spikeDuration", 30).intValue().seconds

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Spike Test")

  val quickBrowsingScenario = scenario("Quick Spike User")
    .exec(http("Quick Browse Books")
      .get("/api/v1/books?page=0&size=20")
      .check(status.is(200))
      .check(responseTimeInMillis.lte(5000)))
    .pause(500.milliseconds)
    .exec(http("Search Popular Book")
      .get("/api/v1/books?title=Alice&page=0&size=20")
      .check(status.in(200, 404)))
    .pause(300.milliseconds)
    .exec(http("Browse Authors")
      .get("/api/v1/authors?page=0&size=20")
      .check(status.is(200)))

  setUp(
    quickBrowsingScenario.inject(
      constantUsersPerSec(normalLoad) during 30.seconds,    // Carico normale
      atOnceUsers(spikeLoad),                               // SPIKE improvviso!
      constantUsersPerSec(spikeLoad) during spikeDuration,  // Mantiene il picco
      rampUsers(normalLoad) during 30.seconds               // Ritorno alla normalità
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(10000),   // Anche sotto spike, max < 10s
      global.successfulRequests.percent.gt(85)  // Almeno 85% di successo
    )
}


