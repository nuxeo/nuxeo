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

  val scnNav = ScnNavigationDownload.get(documents, Parameters.getSimulationDuration(),
    Parameters.getPause(5000, prefix = "nav."))
  val scnQuery = ScnSearch.get(Parameters.getSimulationDuration(),
    Parameters.getPause(5000, prefix = "search."))
  val scnCreate = ScnCreateDocumentsDirectUpload.get(documents, Parameters.getSimulationDuration(),
    Parameters.getPause(5000, prefix = "create."))

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .disableCaching
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connectionHeader("keep-alive")

  setUp(
    scnNav.inject(rampUsers(Parameters.getConcurrentUsers(15, prefix = "nav."))
      .over(Parameters.getRampDuration(prefix = "nav."))).exponentialPauses,
    scnQuery.inject(rampUsers(Parameters.getConcurrentUsers(15, prefix = "search."))
      .over(Parameters.getRampDuration(prefix = "search."))).exponentialPauses,
    scnCreate.inject(rampUsers(Parameters.getConcurrentUsers(20, prefix = "create."))
      .over(Parameters.getRampDuration(prefix = "create."))).exponentialPauses
  ).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(80))
}
