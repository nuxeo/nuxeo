package org.nuxeo.drive.bench


import io.gatling.core.Predef._
import io.gatling.http.Predef._


class DriveBench extends Simulation {


  val url = System.getProperty("url", "http://localhost:8080/nuxeo")
  val nbUsers = Integer.getInteger("users", 100)
  val myRamp = java.lang.Long.getLong("ramp", 10L)
  val myDuration = java.lang.Long.getLong("duration", 60L)
  val pollInterval = Integer.getInteger("pollInterval", 10)
  val thinkTime = Integer.getInteger("pause", 10)

  val scn1Percentage = Integer.getInteger("scn1", 100)

  def getNbUsers(percentage: Int): Int = {
    (nbUsers.intValue * percentage.intValue / 100.0).ceil.toInt
  }

  val httpProtocol = http
    .baseURL(url)
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connection("keep-alive")


  val poll = scenario("Poll Scenario").during(myDuration) {
    exec(PollChanges.run(pollInterval))
  }

  Feeders.clearTokens()

  setUp(
 //   bind.inject(rampUsers(getNbUsers(scn1Percentage.intValue)).over(myRamp)),
    poll.inject(rampUsers(getNbUsers(scn1Percentage.intValue)).over(myRamp))
  ).protocols(httpProtocol)
}
