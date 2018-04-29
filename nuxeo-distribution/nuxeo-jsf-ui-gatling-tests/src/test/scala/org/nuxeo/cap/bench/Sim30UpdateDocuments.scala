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

object ScnUpdateDocuments {

  def get = (documents: Iterator[Map[String, String]], duration: Duration, pause: Duration) => {
    scenario("UpdateDocuments").exec(
      during(duration, "counterName") {
        feed(documents)
          .feed(Feeders.users)
          .exec(NuxeoRest.updateDocument())
          .pause(pause)
      }
    ).feed(Feeders.admins)
  }

}


class Sim30UpdateDocuments extends Simulation {
  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val documents = Feeders.createRandomDocFeeder()
  val scn = ScnUpdateDocuments.get(documents, Parameters.getSimulationDuration(), Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers()).over(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
    .assertions(global.successfulRequests.percent.gte(80))
}
