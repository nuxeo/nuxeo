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

class Sim50Bench extends Simulation {

  val documents = Feeders.createRandomDocFeeder()
  val scnNav = ScnNavigation.get(documents, Parameters.getSimulationDuration(),
    Parameters.getPause(1000, prefix = "nav."))
  val scnUpdate = ScnUpdateDocuments.get(documents, Parameters.getSimulationDuration(),
    Parameters.getPause(500, prefix = "upd."))

  val httpProtocol = http
    .baseUrl(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connectionHeader("keep-alive")

  setUp(
    scnNav.inject(rampUsers(Parameters.getConcurrentUsers(20, prefix = "nav."))
      .during(Parameters.getRampDuration(prefix = "nav."))).exponentialPauses,
    scnUpdate.inject(rampUsers(Parameters.getConcurrentUsers(5, prefix = "upd."))
      .during(Parameters.getRampDuration(prefix = "upd."))).exponentialPauses
  ).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(80))
}
