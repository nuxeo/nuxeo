package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.core.config.GatlingFiles
import io.gatling.http.Predef._

import scala.io.Source

object Setup {

  val run = (userCount: Integer) => {

    feed(Feeders.admins)
      .exec(Actions.createGroupIfNotExists(Constants.GAT_GROUP_NAME)).exitHereIfFailed
      .exec(Actions.createDocumentIfNotExistsAsAdmin(Constants.ROOT_WORKSPACE_PATH, Constants.GAT_WS_NAME, "Workspace")).exitHereIfFailed
      .exec(Actions.grantReadWritePermission(Constants.GAT_WS_PATH, Constants.GAT_GROUP_NAME)).exitHereIfFailed
      .exec(Actions.createDocumentIfNotExistsAsAdmin(Constants.GAT_WS_PATH, Constants.GAT_FOLDER_NAME, "Folder")).exitHereIfFailed
      .repeat(userCount.intValue(), "count") {
      feed(Feeders.usersQueue)
        .feed(Feeders.deviceId)
        .exec(Actions.createUserIfNotExists(Constants.GAT_GROUP_NAME))
        .exec(Actions.createDocumentIfNotExists(Constants.GAT_WS_PATH, Constants.GAT_USER_FOLDER_NAME, "Folder"))
        .exec(Actions.synchronyzeFolder(Constants.GAT_WS_PATH + "/" + Constants.GAT_USER_FOLDER_NAME))
        .exec(Actions.synchronyzeFolder(Constants.GAT_WS_PATH + "/" + Constants.GAT_FOLDER_NAME))
        .exec(Actions.getDriveToken())
    }
  }
}

class SetupSimulation extends Simulation {

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connection("keep-alive")

  val userCount = Source.fromFile(GatlingFiles.dataDirectory + "/users.csv").getLines.size - 1
  val scn = scenario("Setup").exec(Setup.run(userCount))

  Feeders.clearTokens()
  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.is(100))
}
