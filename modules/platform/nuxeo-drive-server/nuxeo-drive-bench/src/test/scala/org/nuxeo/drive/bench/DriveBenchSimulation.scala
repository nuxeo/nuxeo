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
package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._

class Sim10BenchPolling extends Simulation {

  val httpProtocol = http
    .baseUrl(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connectionHeader("keep-alive")
    .disableCaching // disabling Etag cache since If-None-Modified on GetChangeSummary fails

  val poll = scenario("Poll").during(Parameters.getSimulationDuration(60)) {
    exec(PollChanges.run(Parameters.getPollInterval(30)))
  }

  val serverFeeder = scenario("Server Feeder").during(Parameters.getSimulationDuration(60)) {
    exec(ServerFeeder.run(Parameters.getFeederInterval(10)))
  }

  setUp(
    poll.inject(rampUsers(Parameters.getConcurrentUsers(100)).during(Parameters.getRampDuration(10))),
    serverFeeder.inject(rampUsers(Parameters.getConcurrentWriters(10)).during(Parameters.getRampDuration(10))).exponentialPauses
  ).protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.gt(80))
}

class Sim40BenchRecursiveRemoteScan extends Simulation {

  val httpProtocol = http
    .baseUrl(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connectionHeader("keep-alive")
    .disableCaching // disabling Etag cache since If-None-Modified on GetChildren fails

  val remoteScan = RecursiveRemoteScan.run()

  setUp(remoteScan.inject(rampUsers(Parameters.getConcurrentUsers(10, prefix = "remoteScan.")).during(Parameters.getRampDuration(10))))
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.is(100))
}

class Sim45BenchBatchedRemoteScan extends Simulation {

  val httpProtocol = http
    .baseUrl(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connectionHeader("keep-alive")
    .disableCaching // disabling Etag cache since If-None-Modified on GetChildren and ScrollDescendants fails

  val remoteScan = BatchedRemoteScan.run(Parameters.getDescendantsBatchSize(100), Parameters.getPause(100, prefix = "remoteScan."))

  setUp(remoteScan.inject(rampUsers(Parameters.getConcurrentUsers(10, prefix = "remoteScan.")).during(Parameters.getRampDuration(10))))
    .protocols(httpProtocol)
    .assertions(global.successfulRequests.percent.is(100))
}
