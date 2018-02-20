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
package org.nuxeo.cap.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration.Duration

object ScnTest {

  def get = (documents: Iterator[Map[String, String]], duration: Duration, pause: Duration) => {
    scenario("TestS3").exec(
      during(duration, "counterName") {
        feed(documents)
          .feed(Feeders.users)
          .exec(NuxeoRest.s3Upload())
          .pause(pause)
      }
    )
  }
}


class Sim50Test extends Simulation {
  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .disableCaching // needed because s3 does not handle If-None-Match header
    .acceptEncodingHeader("gzip, deflate")
    .connection("keep-alive")
  val documents = Feeders.createRandomDocFeeder()
  val scn = ScnTest.get(documents, Parameters.getSimulationDuration(), Parameters.getPause())
  setUp(scn.inject(rampUsers(1).over(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
    .assertions(global.successfulRequests.percent.greaterThan(70))
}
