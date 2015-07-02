package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.core.config.GatlingFiles
import io.gatling.http.Predef._


import scala.io.Source

object InitUsers {

  val workspaceName = "Bench_Drive"
  val workspacePath = "/Bench_Drive"
  val commonFolder = "Common"

  val run = (userCount: Integer) => {
    group("Init users") {
      feed(Feeders.admins)
        .exec(Actions.createDocumentIfNotExistsAsAdmin("", workspaceName, "Workspace"))
        .exec(Actions.grantWrite(workspacePath, "members"))
        .exec(Actions.createDocumentIfNotExistsAsAdmin(workspacePath, commonFolder, "Folder"))
        .repeat(userCount.intValue(), "count") {
        feed(Feeders.usersQueue)
          .feed(Feeders.deviceId)
          .exec(Actions.createUserIfNotExists())
          .exec(Actions.createDocumentIfNotExists(workspacePath, "Folder_${user}", "Folder"))
          .exec(Actions.synchronyzeFolder("/Bench_Drive/Folder_${user}"))
          .exec(Actions.synchronyzeFolder("/Bench_Drive/Common"))
          .exec(Actions.bindUser())
      }
    }
  }
}

class InitUsersSimulation extends Simulation {
  val url = System.getProperty("url", "http://localhost:8080/nuxeo")

  val httpProtocol = http
    .baseURL(url)
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connection("keep-alive")

  val userCount = Source.fromFile(GatlingFiles.dataDirectory + "/users.csv").getLines.size - 1
  val scn = scenario("Init Users").exec(InitUsers.run(userCount))

  Feeders.clearTokens()
  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
