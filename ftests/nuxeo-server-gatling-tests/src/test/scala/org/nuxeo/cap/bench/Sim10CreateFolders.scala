/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Delbosc Benoit
 */
package org.nuxeo.cap.bench

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
    .baseUrl(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val folders = Feeders.createFolderFeeder()
  val scn = ScnCreateFolders.get(folders, Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers(1)).during(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
    .assertions(global.successfulRequests.percent.is(100))
}
