/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.cap.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object ScnBulkUpdateFolders {

  def get = (documents: Iterator[Map[String, String]], duration: Duration, pause: Duration) => {
    scenario("BulkUpdateFolders").exec(
      during(duration, "counterName") {
        feed(documents)
          // TODO: when NXP-25940 is done use random user
          .feed(Feeders.admins)
          .exec(NuxeoBulk.bulkUpdateDocument("SELECT * FROM Document WHERE ecm:path = '" + Constants.GAT_WS_PATH + "/${parentPath}'", "dc:description", "bulk folder")
            .asJSON.check(jsonPath("$.commandId").saveAs("commandId")))
          .exec(NuxeoBulk.waitForAction("${commandId}"))
          .pause(pause)
      }
    )
  }
}

// Run a bulk update command on a parent folder of a document
class Sim25BulkUpdateFolders extends Simulation {
  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val scn = ScnBulkUpdateFolders.get(documents, Parameters.getSimulationDuration(), Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers()).over(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
    .assertions(global.successfulRequests.percent.gte(90))
}
