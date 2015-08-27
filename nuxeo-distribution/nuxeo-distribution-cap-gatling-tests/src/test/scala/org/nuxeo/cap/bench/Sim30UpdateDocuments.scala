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

class Sim30UpdateDocuments extends Simulation {

  def run = (duration: Int, documents: Iterator[Map[String, String]]) => {
    during(duration, "counterName") {
      feed(documents)
        .feed(Feeders.usersCircular)
        .exec(NuxeoRest.updateDocument())
    }
  }

  val url = System.getProperty("url", "http://localhost:8080/nuxeo")
  val nbUsers = Integer.getInteger("users", 8)
  val duration = Integer.getInteger("duration", 30)
  val httpProtocol = http
    .baseURL(url)
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connection("keep-alive")
  val documents = Feeders.createRandomDocFeeder()
  val scn = scenario("30-UpdateDocuments").exec(run(duration, documents))

  //serverFeeder.inject(rampUsers(nbWriter).over(myRamp))
  setUp(scn.inject(atOnceUsers(nbUsers))).protocols(httpProtocol)
}
