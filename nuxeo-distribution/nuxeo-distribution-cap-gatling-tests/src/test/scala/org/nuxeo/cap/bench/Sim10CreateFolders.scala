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

class Sim10CreateFolders extends Simulation {

  def run = (folders: Iterator[Map[String, String]]) => {
    asLongAs(session => Feeders.notEmpty(session), exitASAP = true) {
      feed(folders)
        .feed(Feeders.usersCircular)
        .exec(NuxeoRest.createDocument())
        .doIf(session => Redis.markFolderCreated(session)) {
        exec()
      }
    }
  }

  val url = System.getProperty("url", "http://localhost:8080/nuxeo")
  val nbUsers = Integer.getInteger("users", 1)
  val httpProtocol = http
    .baseURL(url)
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connection("keep-alive")
  val folders = Feeders.createFolderFeeder()
  val scn = scenario("10-CreateFolders").exec(run(folders))

  setUp(scn.inject(atOnceUsers(nbUsers))).protocols(httpProtocol)
}
