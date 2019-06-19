/*
 * (C) Copyright 2018-2019 Nuxeo (http://nuxeo.com/) and others.
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

object ScnBulkCsvExport {

  def get = () => {
    scenario("BulkCsvExport")
      .feed(Feeders.admins)
      .exec(NuxeoBulk.bulkCsvExport("SELECT * FROM File WHERE ecm:isVersion = 0 AND ecm:isTrashed = 0")
        .check(jsonPath("$.commandId").saveAs("commandId")))
      .exec(NuxeoBulk.waitForAction("${commandId}"))
  }

}

class Sim20CSVExport extends Simulation {
  val httpProtocol = http
    .baseUrl(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val scn = ScnBulkCsvExport.get()
  setUp(scn.inject(atOnceUsers(1)))
    .protocols(httpProtocol)
}
