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

object ScnCreateDocuments {

  def get = (documents: Iterator[Map[String, String]], pause: Duration) => {
    scenario("CreateDocuments").exec(
      asLongAs(session => Feeders.notEmpty(session), exitASAP = true) {
        feed(documents)
          .feed(Feeders.users)
          .exec(NuxeoRest.createDocumentDirectUploadS3())
          .doIf(session => Redis.markDocumentCreated(session)) {
          exec()
        }.pause(pause)
      }
    ).feed(Feeders.admins)
  }

}

object ScnCreateAgainDocuments {

  def get = (documents: Iterator[Map[String, String]], duration: Duration, pause: Duration) => {
    scenario("CreateAgainDocuments").exec(
      during(duration, "counterName") {
        feed(documents)
          .feed(Feeders.users)
          .exec(NuxeoRest.createAgainDocument())
          .pause(pause)
      }
    ).feed(Feeders.admins)
  }
}

object ScnCreateDocumentsDirectUpload {

  def get = (documents: Iterator[Map[String, String]], duration: Duration, pause: Duration) => {
    scenario("DirectUpload").exec(
      during(duration, "counterName") {
        feed(documents)
          .feed(Feeders.users)
          .exec(NuxeoRest.createDocumentDirectUploadS3("Upload: "))
          .pause(pause)
      }
    ).feed(Feeders.admins)
  }
}

class Sim20CreateDocuments extends Simulation {

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableCaching // because of s3 upload
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connection("keep-alive")
  val documents = Feeders.createDocFeeder()
  val scn = ScnCreateDocuments.get(documents, Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers()).over(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
    .assertions(global.successfulRequests.percent.greaterThan(90))
}
