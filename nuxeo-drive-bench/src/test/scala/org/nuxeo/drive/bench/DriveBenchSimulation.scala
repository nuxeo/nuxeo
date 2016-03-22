package org.nuxeo.drive.bench


import io.gatling.core.Predef._
import io.gatling.http.Predef._


class DriveBenchSimulation extends Simulation {

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connection("keep-alive")
    .disableCaching // disabling Etag cache since If-None-Modified on GetChangeSummary fails

  val poll = scenario("Poll").during(Parameters.getSimulationDuration(60)) {
    exec(PollChanges.run(Parameters.getPollInterval(30)))
  }

  val serverFeeder = scenario("Server Feeder").during(Parameters.getSimulationDuration(60)) {
    exec(ServerFeeder.run(Parameters.getFeederInterval(10)))
  }

  setUp(
    poll.inject(rampUsers(Parameters.getConcurrentUsers(100)).over(Parameters.getRampDuration(10))),
    serverFeeder.inject(rampUsers(Parameters.getConcurrentWriters(10)).over(Parameters.getRampDuration(10))).exponentialPauses
  ).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.greaterThan(80))
}
