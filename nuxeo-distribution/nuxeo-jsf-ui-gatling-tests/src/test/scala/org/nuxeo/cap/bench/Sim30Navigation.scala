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
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val documents = Feeders.createRandomDocFeeder()
  val scn = ScnNavigation.get(documents, Parameters.getSimulationDuration(), Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers()).over(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
}
