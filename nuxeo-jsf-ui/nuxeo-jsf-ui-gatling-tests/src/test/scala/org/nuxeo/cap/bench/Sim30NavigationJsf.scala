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
package org.nuxeo.cap.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.Duration


object ScnNavigationJsf {

  def get = (documents: Iterator[Map[String, String]], duration: Duration, pause: Duration) => {
    val tabPause = pause / 4
    scenario("NavigationJsf").exec(
      during(duration, "counterName") {
        feed(Feeders.users)
          .exec(NuxeoJsf.loginAndGoToGatlingWorkspace())
          .pause(pause)
          .feed(documents)
          .exec(NuxeoJsf.viewParentFolderOfCurrentDocument())
          .pause(pause)
          .exec(NuxeoJsf.viewCurrentDocument())
          .pause(pause)
          .exec(NuxeoJsf.viewCurrentDocumentEditTab())
          .pause(tabPause)
          .exec(NuxeoJsf.viewCurrentDocumentFilesTab())
          .pause(tabPause)
          .exec(NuxeoJsf.viewCurrentDocumentPermissionTab())
          .pause(tabPause)
          .exec(NuxeoJsf.viewCurrentDocumentPublishTab())
          .pause(tabPause)
          .exec(NuxeoJsf.viewCurrentDocumentRelationTab())
          .pause(tabPause)
          .exec(NuxeoJsf.viewCurrentDocumentCommentsTab())
          .pause(tabPause)
          .exec(NuxeoJsf.viewCurrentDocumentHistoryTab())
          .pause(tabPause)
          .repeat(5) {
          feed(documents)
            .exec(NuxeoJsf.viewParentFolderOfCurrentDocument())
            .pause(pause)
            .exec(NuxeoJsf.viewCurrentDocument())
            .pause(pause)
        }.exec(NuxeoJsf.logout())
      }
    )
  }

}


class Sim30NavigationJsf extends Simulation {
  val httpProtocol = http
    .baseUrl(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val documents = Feeders.createRandomDocFeeder()
  val scn = ScnNavigationJsf.get(documents, Parameters.getSimulationDuration(), Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers()).during(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
}
