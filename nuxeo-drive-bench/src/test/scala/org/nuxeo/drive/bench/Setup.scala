package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.core.config.GatlingFiles
import io.gatling.http.Predef._

import scala.io.Source

object Setup {


  val run = (userCount: Integer) => {

    feed(Feeders.admins)
      .exec(Actions.createGroupIfNotExists(Const.groupName))
      .exec(Actions.createDocumentIfNotExistsAsAdmin("", Const.workspaceName, "Workspace"))
      .exec(Actions.grantReadWritePermission(Const.workspacePath, Const.groupName))
      .exec(Actions.createDocumentIfNotExistsAsAdmin(Const.workspacePath, Const.commonFolder, "Folder"))
      .repeat(userCount.intValue(), "count") {
      feed(Feeders.usersQueue)
        .feed(Feeders.deviceId)
        .exec(Actions.createUserIfNotExists(Const.groupName))
        .exec(Actions.createDocumentIfNotExists(Const.workspacePath, Const.userFolder, "Folder"))
        .exec(Actions.synchronyzeFolder(Const.workspacePath + "/" + Const.userFolder))
        .exec(Actions.synchronyzeFolder(Const.workspacePath + "/" + Const.commonFolder))
        .exec(Actions.getDriveToken())
    }
  }
}

class SetupSimulation extends Simulation {
  val url = System.getProperty("url", "http://localhost:8080/nuxeo")

  val httpProtocol = http
    .baseURL(url)
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connection("keep-alive")

  val userCount = Source.fromFile(GatlingFiles.dataDirectory + "/users.csv").getLines.size - 1
  val scn = scenario("Setup").exec(Setup.run(userCount))

  Feeders.clearTokens()
  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
