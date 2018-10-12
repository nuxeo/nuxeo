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

  def get = (docs: Iterator[Map[String, String]]) => {
    scenario("BulkUpdateFolders").exec(
      asLongAs(session => Feeders.notEmpty(session), exitASAP = true) {
        feed(Feeders.admins)
          .feed(docs)
          .exec(NuxeoBulk.bulkUpdateDocument("SELECT * FROM Document WHERE ecm:path = '" + Constants.GAT_WS_PATH + "/${parentPath}'", "dc:description", "bulk folder")
            .asJSON.check(jsonPath("$.commandId").saveAs("commandId")))
          .exec(NuxeoBulk.waitForAction("${commandId}"))
      }
    )
  }

}
// Run a bulk command for each document matching the parent folder
class Sim25BulkUpdateFolders extends Simulation {
  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val docs = Feeders.createDocFeeder()
  val scn = ScnBulkUpdateFolders.get(docs)
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers()).over(Parameters.getRampDuration())))
    .protocols(httpProtocol)
}
