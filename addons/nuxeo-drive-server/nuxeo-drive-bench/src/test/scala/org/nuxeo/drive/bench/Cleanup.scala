package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.core.config.GatlingFiles
import io.gatling.http.Predef._

import scala.io.Source

object Cleanup {

  val run = (userCount: Integer) => {
    feed(Feeders.admins)
      .exec(Actions.deleteFileDocumentAsAdmin(Const.workspacePath))
      .repeat(userCount.intValue(), "count") {
      feed(Feeders.usersQueue)
        .exec(Actions.deleteUser())
    }.exec(Actions.deleteGroup(Const.groupName))
  }
}

class CleanupSimulation extends Simulation {
  val url = System.getProperty("url", "http://localhost:8080/nuxeo")

  val httpProtocol = http
    .baseURL(url)
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connection("keep-alive")

  val userCount = Source.fromFile(GatlingFiles.dataDirectory + "/users.csv").getLines.size - 1
  val scn = scenario("Cleanup").exec(Cleanup.run(userCount))

  Feeders.clearTokens()
  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
