package org.nuxeo.drive.bench


import io.gatling.core.Predef._
import io.gatling.http.Predef._


class Sim10BenchPolling extends Simulation {

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connectionHeader("keep-alive")
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

class Sim40BenchRecursiveRemoteScan extends Simulation {

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connectionHeader("keep-alive")
    .disableCaching // disabling Etag cache since If-None-Modified on GetChildren fails

  val remoteScan = RecursiveRemoteScan.run()

  setUp(remoteScan.inject(rampUsers(Parameters.getConcurrentUsers(10, prefix = "remoteScan.")).over(Parameters.getRampDuration(10))))
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.is(100))
}

class Sim45BenchBatchedRemoteScan extends Simulation {

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connectionHeader("keep-alive")
    .disableCaching // disabling Etag cache since If-None-Modified on GetChildren and ScrollDescendants fails

  val remoteScan = BatchedRemoteScan.run(Parameters.getDescendantsBatchSize(100), Parameters.getPause(100, prefix = "remoteScan."))

  setUp(remoteScan.inject(rampUsers(Parameters.getConcurrentUsers(10, prefix = "remoteScan.")).over(Parameters.getRampDuration(10))))
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.is(100))
}
