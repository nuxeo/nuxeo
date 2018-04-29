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
import io.gatling.http.Predef._

import scala.concurrent.duration.Duration

object ScnCreateFolders {

  def get = (folders: Iterator[Map[String, String]], pause: Duration) => {
    scenario("CreateFolders").exec(
      asLongAs(session => Feeders.notEmpty(session), exitASAP = true) {
        feed(folders)
          .feed(Feeders.users)
          .exec(NuxeoRest.createDocument())
          .doIf(session => Redis.markFolderCreated(session)) {
          exec()
        }.pause(pause)
      }
    ).feed(Feeders.admins).exec(NuxeoRest.waitForAsyncJobs())
  }

}

class Sim10CreateFolders extends Simulation {
  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val folders = Feeders.createFolderFeeder()
  val scn = ScnCreateFolders.get(folders, Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers(1)).over(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
    .assertions(global.successfulRequests.percent.is(100))
}
