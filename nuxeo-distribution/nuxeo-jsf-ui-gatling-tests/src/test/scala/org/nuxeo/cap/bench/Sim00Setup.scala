package org.nuxeo.cap.bench

/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Delbosc Benoit
 */

import io.gatling.core.Predef._
import io.gatling.core.config.GatlingFiles
import io.gatling.http.Predef._

import scala.concurrent.duration.Duration
import scala.io.Source

object ScnSetup {

  def get = (userCount: Integer, pause: Duration) => {
    scenario("Setup").exec(
      feed(Feeders.admins)
        .exec(NuxeoRest.createGroupIfNotExists(Constants.GAT_GROUP_NAME)).exitHereIfFailed
        .exec(NuxeoRest.createDocumentIfNotExistsAsAdmin(Constants.ROOT_WORKSPACE_PATH, Constants.GAT_WS_NAME, "Workspace")).exitHereIfFailed
        .doIf(session => session("status").as[Integer].equals(201)) {
          exec(NuxeoRest.grantReadWritePermission(Constants.GAT_WS_PATH, Constants.GAT_GROUP_NAME)).exitHereIfFailed
        }
        .exec(NuxeoRest.createDocumentIfNotExistsAsAdmin(Constants.GAT_WS_PATH, Constants.GAT_FOLDER_NAME, "Folder")).exitHereIfFailed
        .repeat(userCount.intValue(), "count") {
        feed(Feeders.users)
          .exec(NuxeoRest.createUserIfNotExists(Constants.GAT_GROUP_NAME)).pause(pause)
        //.exec(Actions.createDocumentIfNotExists(Constants.GAT_WS_PATH, Constants.GAT_USER_FOLDER_NAME, "Folder"))
      }
    )
  }

}

class Sim00Setup extends Simulation {
  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val userCount = Source.fromFile(GatlingFiles.dataDirectory + "/users.csv").getLines.size - 1
  val scn = ScnSetup.get(userCount, Parameters.getPause())
  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol).exponentialPauses
    .assertions(global.successfulRequests.percent.is(100))
}
