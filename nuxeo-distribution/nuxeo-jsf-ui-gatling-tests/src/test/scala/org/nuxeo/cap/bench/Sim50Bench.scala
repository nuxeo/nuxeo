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

class Sim50Bench extends Simulation {

  val documents = Feeders.createRandomDocFeeder()
  val scnNav = ScnNavigation.get(documents, Parameters.getSimulationDuration(),
    Parameters.getPause(1000, prefix = "nav."))
  val scnNavJsf = ScnNavigationJsf.get(documents, Parameters.getSimulationDuration(),
    Parameters.getPause(1000, prefix = "navjsf."))
  val scnUpdate = ScnUpdateDocuments.get(documents, Parameters.getSimulationDuration(),
    Parameters.getPause(500, prefix = "upd."))

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connectionHeader("keep-alive")

  setUp(
    scnNav.inject(rampUsers(Parameters.getConcurrentUsers(20, prefix = "nav."))
      .over(Parameters.getRampDuration(prefix = "nav."))).exponentialPauses,
    scnNavJsf.inject(rampUsers(Parameters.getConcurrentUsers(10, prefix = "navjsf."))
      .over(Parameters.getRampDuration(prefix = "navjsf."))).exponentialPauses,
    scnUpdate.inject(rampUsers(Parameters.getConcurrentUsers(5, prefix = "upd."))
      .over(Parameters.getRampDuration(prefix = "upd."))).exponentialPauses
  ).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gte(80))
}
