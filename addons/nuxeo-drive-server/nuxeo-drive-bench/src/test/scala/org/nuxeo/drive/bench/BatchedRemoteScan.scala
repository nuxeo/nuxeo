/*
 * (C) Copyright 2016-2019 Nuxeo (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration

object BatchedRemoteScan {

  val logger = Logger(LoggerFactory.getLogger(getClass))

  /**
   * Let's simultate Nuxeo Drive batched remote scan.
   */
  def run = (batchSize: Integer, thinkTime: Duration) => {
    scenario("BatchedRemoteScan")
    .group("BatchedRemoteScan") {
      feed(Feeders.token)
        .exec(Actions.fetchAutomationAPI())
        // Get top level folder
        .exec(Actions.getTopLevelFolder().asJson
          .check(jsonPath("$.id").saveAs("topLevelFolderId"))
          .check(jsonPath("$.canScrollDescendants").ofType[Boolean].saveAs("canScrollDescendants")))
        .exec(Actions.getFileSystemItem("${topLevelFolderId}"))
        // Initial call to GetChangeSummary
        .exec(Actions.getChangeSummary(None, None).asJson
          .check(jsonPath("$.upperBound").saveAs("upperBound"))
          .check(jsonPath("$.activeSynchronizationRootDefinitions").saveAs("roots")))
        // Calls to GetChildren or ScrollDescendants depending on the node type
        .exec(session => session.set("nodes", List(Map("id" -> session("topLevelFolderId").as[String],
          "canScrollDescendants" -> session("canScrollDescendants").as[Boolean]))))
        .asLongAs(session => session("nodes").as[List[Map[String, Any]]].nonEmpty) {
          exec(session => {
            logger.debug("nodes = " + session("nodes").as[List[Map[String, Any]]])
            session})
          // Pop node from nodes list
          .exec(session => session.set("nodeId", session("nodes").as[List[Map[String, Any]]].head("id"))
            .set("canScrollDescendants", session("nodes").as[List[Map[String, Any]]].head("canScrollDescendants")))
          .doIfOrElse(session => session("canScrollDescendants").as[Boolean]) {
            // Scroll through node descendants by batch
            exec(session => session.set("scrollId", ""))
            .asLongAs(session => session("scrollId").asOption[String].isDefined) {
              exec(session => {
                logger.debug("Calling ScrollDescendants on nodeId " + session("nodeId").as[String] + " with scroll id [" + session("scrollId").as[String] + "] and batch size [" + batchSize + "]")
                session})
              .exec(
                Actions.scrollDescendants("${nodeId}", "${scrollId}", batchSize.toString).asJson
                  .check(jsonPath("$.scrollId").saveAs("scrollId"))
                  .check(jsonPath("$.fileSystemItems[*]").ofType[Map[String, Any]].findAll
                    .transformOption(extract => extract match {
                      case None => Some(Vector.empty)
                      case descendants => descendants
                    }).saveAs("descendants")))
              .pause(thinkTime)
              .doIf(session => session("descendants").as[Vector[Map[String, Any]]].isEmpty) {
                exec(session => session.remove("scrollId"))
              }
            }
            .exec(session => session.set("nodes", session("nodes").as[List[Map[String, Any]]].tail))
          } {
            // Get node children
            exec(session => {
              logger.debug("Calling GetChildren on nodeId " + session("nodeId").as[String])
              session})
            .exec(Actions.getChildren("${nodeId}").asJson
              .check(jsonPath("$[*]").ofType[Map[String, Any]].findAll
                .transformOption(extract => extract match {
                  case None => Some(Vector.empty)
                  case children => children
                }).saveAs("children")))
            // Add foldersih children nodes to nodes list
            .exec(session => session.set("nodes",
              session("children").as[Vector[Map[String, Any]]].filter(_("folder").asInstanceOf[Boolean]).map(_.filterKeys(Set("id", "canScrollDescendants")))
              ++ session("nodes").as[List[Map[String, Any]]].tail))
          }
        }
    }
  }
}
