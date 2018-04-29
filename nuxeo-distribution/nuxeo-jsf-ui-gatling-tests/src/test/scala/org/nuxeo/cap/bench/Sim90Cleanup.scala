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

import scala.io.Source

class Sim90Cleanup extends Simulation {
  def run = (userCount: Integer) => {
    feed(Feeders.admins)
      .exec(NuxeoRest.deleteFileDocumentAsAdmin(Constants.GAT_WS_PATH))
      .repeat(userCount.intValue(), "count") {
      feed(Feeders.users)
        .exec(NuxeoRest.deleteUser())
    }.exec(NuxeoRest.deleteGroup(Constants.GAT_GROUP_NAME))
  }

  val url = System.getProperty("url", "http://localhost:8080/nuxeo")
  val httpProtocol = http
    .baseURL(url)
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val userCount = Source.fromFile(GatlingFiles.dataDirectory + "/users.csv").getLines.size - 1
  val scn = scenario("Cleanup").exec(run(userCount))
  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
