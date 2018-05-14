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
import scala.concurrent.duration.Duration

object ScnNavigationSpike {

  def get = (documents: Iterator[Map[String, String]], duration: Duration, pause: Duration) => {
    scenario("NavigationRestSpike").exec(
      during(duration, "counterName") {
        feed(Feeders.users).repeat(5) {
          feed(documents)
            .randomSwitch(
              30.0 -> exec(NuxeoRest.getDocument("Get document")),
              10.0 -> exec(NuxeoRest.getParentFolderOfCurrentDocument("Get document folder")),
              10.0 -> exec(NuxeoRest.downloadBlob()),
              10.0 -> exec(NuxeoRest.getDocument("Get document with acl", enrichers = "acl")),
              10.0 -> exec(NuxeoRest.getDocument("Get document with thumbnail", enrichers = "thumbnail")),
              10.0 -> exec(NuxeoRest.getDocument("Get document with properties", parts = "properties")),
              5.0 -> exec(NuxeoRest.getDocument("Get document dc only", schemas = "dublincore")),
              5.0 -> exec(NuxeoRest.getDocument("Get document with breadcrumb", enrichers = "breadcrumb")),
              5.0 -> exec(NuxeoRest.getDocument("Get document with versionLabel", parts = "versionLabel")),
              5.0 -> exec(NuxeoRest.getDocument("Get document with lock", parts = "lock"))
            )
            .pause(pause)
        }
      }
    )
  }
}

object ScnSearchSpike {
  def get = (duration: Duration, pause: Duration) => {
    scenario("SearchRestSpike").exec(
      during(duration, "counterName") {
        feed(Feeders.users).repeat(10) {
          feed(Feeders.fulltextSearch)
            .randomSwitch(
              10.0 -> exec(NuxeoRest.search("SELECT * FROM Document", comment = "Search: All")),
              10.0 -> exec(NuxeoRest.search("SELECT * FROM Document", sortBy = "dc:modified", sortOrder = "DESC", comment = "Search: All sorted")),
              10.0 -> exec(NuxeoRest.search("SELECT * FROM Document WHERE ecm:primaryType='Folder'", sortBy = "dc:title", comment = "Search: Folders sorted")),
              10.0 -> exec(NuxeoRest.search("SELECT * FROM Document WHERE ecm:path STARTSWITH '/default-domain/workspaces/Bench_Gatling' AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted'", comment = "Search: With path")),
              60.0 -> exec(NuxeoRest.search("SELECT * FROM Document WHERE ecm:fulltext.dc:title = '${term}' AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted'", comment = "Search: Fulltext"))
            )
            .pause(pause)
        }
      }
    )
  }
}

object ScnSearchSpike2 {

  def get = (duration: Duration, pause: Duration) => {
    scenario("SearchRestSpike").exec(
      during(duration, "counterName") {
        feed(Feeders.users).repeat(10) {
          feed(Feeders.fulltextSearch)
            .randomSwitch(
              40.0 -> exec(NuxeoRest.search("SELECT * FROM Document WHERE ecm:fulltext = '${term}'", comment = "Search Simple")),
              30.0 -> exec(NuxeoRest.search("SELECT * FROM Document WHERE ecm:fulltext = '${term}' AND dc:nature = '${nature}' AND dc:language = '${language}' AND dc:coverage = '${coverage}'", sortBy = "dc:modified", sortOrder = "DESC", comment = "Search Medium")),
              30.0 -> exec(NuxeoRest.search("SELECT * FROM Document WHERE ecm:fulltext = '${term}' AND dc:nature = '${nature}' AND dc:language = '${language}' AND dc:coverage = '${coverage}' AND ecm:mixinType != 'HiddenInNavigation' AND ecm:isProxy = 0 AND ecm:isVersion = 0 AND ecm:currentLifeCycleState != 'deleted'", sortBy = "dc:title", comment = "Search Complex"))
            )
            .pause(pause)
        }
      }
    )
  }
}


class Sim50Bench3 extends Simulation {
  val documents = Feeders.createRandomDocFeeder()

  val scnNav = ScnNavigation.get(documents, Parameters.getSimulationDuration(),
    Parameters.getPause(500, prefix = "nav."))
  val scnNavSpike = ScnNavigationSpike.get(documents, Duration(180, "second"),
    Parameters.getPause(500, prefix = "nav."))
  val scnQuery = ScnSearch.get(Parameters.getSimulationDuration(),
    Parameters.getPause(500, prefix = "search."))
  val scnQuerySpike = ScnSearchSpike.get(Duration(180, "second"),
    Parameters.getPause(500, prefix = "search."))

  val httpProtocol = http
    .baseURL(Parameters.getBaseUrl())
    .disableWarmUp
    .disableCaching
    .acceptEncodingHeader("gzip, deflate")
    .acceptEncodingHeader("identity")
//    .connectionHeader("keep-alive")
    .connection("keep-alive")

  setUp(
    scnNav.inject(rampUsers(100).over(60)).exponentialPauses,
    scnQuery.inject(rampUsers(100).over(60)).exponentialPauses,
    scnQuerySpike.inject(nothingFor(300), rampUsers(Parameters.getConcurrentUsers(800)).over(10)).exponentialPauses,
    scnNavSpike.inject(nothingFor(720), rampUsers(Parameters.getConcurrentUsers(800)).over(10)).exponentialPauses
  ).protocols(httpProtocol)

}
