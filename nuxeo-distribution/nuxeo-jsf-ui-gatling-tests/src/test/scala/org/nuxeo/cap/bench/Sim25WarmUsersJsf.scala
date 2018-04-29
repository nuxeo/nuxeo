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
import io.gatling.core.config.GatlingFiles
import io.gatling.http.Predef._

import scala.io.Source

object ScnWarmupUsersJsf {

  def get = (userCount: Integer) => {
    scenario("WarmUsersJsf").exec(
      repeat(userCount.intValue(), "count") {
        feed(Feeders.users)
          .exec(NuxeoJsf.loginAndGoToGatlingWorkspace()).exec(NuxeoJsf.logout())
      }
    )
  }
}

class Sim25WarmUsersJsf extends Simulation {
  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val userCount = Source.fromFile(GatlingFiles.dataDirectory + "/users.csv").getLines.size - 1
  val scn = ScnWarmupUsersJsf.get(userCount)
  setUp(scn.inject(atOnceUsers(1)))
    .protocols(httpProtocol).exponentialPauses
}
