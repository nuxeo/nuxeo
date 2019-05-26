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

object ScnCRUD {

  def get = (documents: Iterator[Map[String, String]], duration: Duration, pause: Duration) => {
    scenario("DeleteCreateUpdateReadDocuments").exec(
      during(duration, "counterName") {
        feed(documents)
          .feed(Feeders.users)
          .exec(NuxeoRest.deleteDocument())
          .pause(pause)
          .exec(NuxeoRest.createDocument())
          .pause(pause)
          .exec(NuxeoRest.updateDocument())
          .pause(pause)
          .exec(NuxeoRest.getDocument())
          .pause(pause)
      }
    )
  }

}


class Sim50CRUD extends Simulation {
  val httpProtocol = http
    .baseUrl(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val documents = Feeders.createRandomDocFeeder()
  val scn = ScnCRUD.get(documents, Parameters.getSimulationDuration(), Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers()).during(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
    .assertions(global.successfulRequests.percent.gte(70))
}
