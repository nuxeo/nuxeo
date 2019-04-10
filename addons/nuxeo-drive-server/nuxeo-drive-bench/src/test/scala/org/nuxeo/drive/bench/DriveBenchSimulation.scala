package org.nuxeo.drive.bench


import io.gatling.core.Predef._
import io.gatling.http.Predef._


class DriveBenchSimulation extends Simulation {

  val url = System.getProperty("url", "http://localhost:8080/nuxeo")
  val nbUsers = Integer.getInteger("users", 100)
  val nbWriter = Integer.getInteger("writers", 10)
  val myRamp = java.lang.Long.getLong("ramp", 10L)
  val myDuration = java.lang.Long.getLong("duration", 60L)
  val pollInterval = Integer.getInteger("pollInterval", 30)
  val feederInterval = Integer.getInteger("feederInterval", 10)

  val httpProtocol = http
    .baseURL(url)
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connection("keep-alive")
    .disableCaching // disabling Etag cache since If-None-Modified on GetChangeSummary fails

  val poll = scenario("Poll").during(myDuration) {
    exec(PollChanges.run(pollInterval))
  }

  val serverFeeder = scenario("Server Feeder").during(myDuration) {
    exec(ServerFeeder.run(feederInterval))
  }

  setUp(
    poll.inject(rampUsers(nbUsers).over(myRamp)),
    serverFeeder.inject(rampUsers(nbWriter).over(myRamp)).exponentialPauses
  ).protocols(httpProtocol)
}
