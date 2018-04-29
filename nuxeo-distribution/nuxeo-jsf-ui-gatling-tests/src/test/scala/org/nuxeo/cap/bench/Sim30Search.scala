package org.nuxeo.cap.bench

/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration.Duration

object ScnSearch {

  def get = (duration: Duration, pause: Duration) => {
    scenario("SearchRest").exec(
      during(duration, "counterName") {
        feed(Feeders.users).repeat(10) {
          feed(Feeders.fulltextSearch)
            .randomSwitch(
              10.0 -> exec(NuxeoRest.search("SELECT * FROM Document", comment = "Search All")),
              10.0 -> exec(NuxeoRest.search("SELECT * FROM Document", sortBy = "dc:modified", sortOrder = "DESC", comment = "Search All sorted")),
              10.0 -> exec(NuxeoRest.search("SELECT * FROM Document WHERE ecm:primaryType='Folder'", sortBy = "dc:title", comment = "Search folders sorted")),
              10.0 -> exec(NuxeoRest.search("SELECT * FROM Document WHERE ecm:path STARTSWITH '/default-domain/workspaces/Bench_Gatling/u09/t/g/f' AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isVersion = 0 AND ecm:currentLifeCycleState != 'deleted'", comment = "Search on path")),
              60.0 -> exec(NuxeoRest.search("SELECT * FROM Document WHERE dc:title.fulltext = '${term}' AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isVersion = 0 AND ecm:currentLifeCycleState != 'deleted'", comment = "Search fulltext"))
           )
            .pause(pause)
        }
      }
    )
  }

}

class Sim30Search extends Simulation {

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .acceptEncodingHeader("gzip, deflate")
    .connectionHeader("keep-alive")
  val scn = ScnSearch.get(Parameters.getSimulationDuration(), Parameters.getPause())
  setUp(scn.inject(rampUsers(Parameters.getConcurrentUsers()).over(Parameters.getRampDuration())))
    .protocols(httpProtocol).exponentialPauses
}
