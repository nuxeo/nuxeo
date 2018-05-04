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

class Sim50Bench2 extends Simulation {
  val documents = Feeders.createRandomDocFeeder()

  val scnNav = ScnNavigationDownload.get(documents, Parameters.getSimulationDuration(1800),
    Parameters.getPause(4000, prefix = "nav."))
  val scnQuery = ScnSearch.get(Parameters.getSimulationDuration(1800),
    Parameters.getPause(4000, prefix = "search."))
  val scnCreate = ScnCreateAgainDocuments.get(documents, Parameters.getSimulationDuration(1800),
    Parameters.getPause(4000, prefix = "create."))

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .disableCaching
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connectionHeader("keep-alive")

  setUp(
    scnNav.inject(rampUsers(30*6).over(1500)).exponentialPauses,
    scnQuery.inject(rampUsers(30*6).over(1500)).exponentialPauses,
    scnCreate.inject(rampUsers(40*6).over(1500)).exponentialPauses
  ).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(90))
}
