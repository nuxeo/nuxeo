/*
 * (C) Copyright 2015-2026 Nuxeo (http://nuxeo.com/) and contributors.
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
 *     bdelbosc
 */
package org.nuxeo.cap.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.FiniteDuration

/**
 * Sets user preferences on the parent folder of every document created by Sim20CreateDocuments.
 *
 * Runs after Sim25WaitForAsync: unlike document creation, setting a preference is a synchronous REST call with
 * no async indexing to wait for, so it doesn't need to wait itself, but it must run before Sim30Navigation, which
 * relies on preferences already being set (through its preferences enricher).
 */
object ScnSetPreferences {

  def get = (documents: Iterator[Map[String, String]], pause: FiniteDuration) => {
    scenario("SetPreferences").exec(
      asLongAs(session => Feeders.notEmpty(session), exitASAP = true) {
        feed(documents)
          .feed(Feeders.users)
          .exec(NuxeoRest.addPreferencesOnParent())
          .exec(session => Redis.markPreferencesSet(session))
          .pause(pause)
      }
    ).feed(Feeders.admins)
  }

}


class Sim27SetPreferences extends Simulation {

  val httpProtocol = http
    .baseUrl(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val documents = Feeders.createPreferencesFeeder()
  val scn = ScnSetPreferences.get(documents, Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers()).during(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
    .assertions(global.successfulRequests.percent.gte(90))
}
