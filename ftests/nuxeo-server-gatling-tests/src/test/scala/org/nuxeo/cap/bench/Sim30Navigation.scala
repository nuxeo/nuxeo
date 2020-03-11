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

object ScnNavigation {

  def get = (documents: Iterator[Map[String, String]], duration: Duration, pause: Duration) => {
    scenario("NavigationRest").exec(
      during(duration, "counterName") {
        feed(Feeders.users).repeat(5) {
          feed(documents)
            .randomSwitch(
              30.0 -> exec(NuxeoRest.getDocument("Get document")),
              10.0 -> exec(NuxeoRest.getParentFolderOfCurrentDocument("Get document folder")),
              10.0 -> exec(NuxeoRest.getDocument("Get document dc only", schemas = "dublincore")),
              10.0 -> exec(NuxeoRest.getDocument("Get document with acl", enrichers = "acl")),
              10.0 -> exec(NuxeoRest.getDocument("Get document with breadcrumb", enrichers = "breadcrumb")),
              10.0 -> exec(NuxeoRest.getDocument("Get document with thumbnail", enrichers = "thumbnail")),
              10.0 -> exec(NuxeoRest.getDocument("Get document with properties", parts = "properties")),
              5.0 -> exec(NuxeoRest.getDocument("Get document with versionLabel", parts = "versionLabel")),
              5.0 -> exec(NuxeoRest.getDocument("Get document with lock", parts = "lock"))
            )
            .pause(pause)
        }
      }
    )
  }

}

class Sim30Navigation extends Simulation {

  val httpProtocol = http
    .baseUrl(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val documents = Feeders.createRandomDocFeeder()
  val scn = ScnNavigation.get(documents, Parameters.getSimulationDuration(), Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers()).during(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
}
