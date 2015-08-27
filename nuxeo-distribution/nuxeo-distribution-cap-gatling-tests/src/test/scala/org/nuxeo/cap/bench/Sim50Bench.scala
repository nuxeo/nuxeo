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
import io.gatling.redis.util.RedisHelper
class Sim50Bench extends Simulation {
RedisHelper.generateRedisProtocol("foo")
  val url = System.getProperty("url", "http://localhost:8080/nuxeo")
  val nbUsers = Integer.getInteger("users", 100)
  val nbWriter = Integer.getInteger("writers", 10)
  val myRamp = java.lang.Long.getLong("ramp", 10L)
  val myDuration = java.lang.Long.getLong("duration", 60L)
  val pollInterval = Integer.getInteger("pollInterval", 30)
  val feederInterval = Integer.getInteger("feederInterval", 10)

  val httpProtocol = http
    .baseURL(url)
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
    .connection("keep-alive")
    .disableCaching // disabling Etag cache since If-None-Modified on GetChangeSummary fails
/*
  val poll = scenario("Poll").during(myDuration) {
    exec(PollChanges.run(pollInterval))
  }

  val serverFeeder = scenario("Server Feeder").during(myDuration) {
    exec(ServerFeeder.run(feederInterval))
  }
*/
  setUp(
    //serverFeeder.inject(rampUsers(nbWriter).over(myRamp)).exponentialPauses
  ).protocols(httpProtocol)

}
