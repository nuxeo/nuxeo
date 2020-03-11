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

object RecursiveRemoteScan {

  val logger = Logger(LoggerFactory.getLogger(getClass))

  /**
   * Let's simultate Nuxeo Drive recursive remote scan.
   */
  def run = () => {
    scenario("RecursiveRemoteScan")
    .group("RecursiveRemoteScan") {
      feed(Feeders.token)
        .exec(Actions.fetchAutomationAPI())
        // Get top level folder
        .exec(Actions.getTopLevelFolder().asJson.check(jsonPath("$.id").saveAs("topLevelFolderId")))
        .exec(Actions.getFileSystemItem("${topLevelFolderId}"))
        // Initial call to GetChangeSummary
        .exec(Actions.getChangeSummary(None, None).asJson
          .check(jsonPath("$.upperBound").saveAs("upperBound"))
          .check(jsonPath("$.activeSynchronizationRootDefinitions").saveAs("roots")))
        // Recursive calls to GetChildren
        .exec(session => session.set("nodeIds", List(session("topLevelFolderId").as[String])))
        .asLongAs(session => session("nodeIds").as[List[String]].nonEmpty) {
          exec(session => {
            logger.debug("nodeIds = " + session("nodeIds").as[List[String]])
            session})
          // Pop nodeId from nodeIds list and get its children nodes
          .exec(session => session.set("nodeId", session("nodeIds").as[List[String]].head))
          .exec(session => {
            logger.debug("Calling GetChildren on nodeId " + session("nodeId").as[String])
            session})
          .exec(Actions.getChildren("${nodeId}").asJson
            .check(jsonPath("$[*]").ofType[Map[String, Any]].findAll
              .transformOption(extract => extract match {
                case None => Some(Vector.empty)
                case children => children
              }).saveAs("children")))
          // Add foldersih children node ids to nodeIds list
          .exec(session => session.set("nodeIds",
            session("children").as[Vector[Map[String, Any]]].filter(_("folder").asInstanceOf[Boolean]).map(_("id"))
            ++ session("nodeIds").as[List[String]].tail))
        }
    }
  }
}
