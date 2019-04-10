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
        .exec(Actions.getTopLevelFolder().asJSON.check(jsonPath("$.id").saveAs("topLevelFolderId")))
        .exec(Actions.getFileSystemItem("${topLevelFolderId}"))
        // Initial call to GetChangeSummary
        .exec(Actions.getChangeSummary(None, None).asJSON
          .check(jsonPath("$.upperBound").saveAs("upperBound"))
          .check(jsonPath("$.activeSynchronizationRootDefinitions").saveAs("roots")))
        // Recursive calls to GetChildren
        .exec(session => session.set("nodeIds", List(session("topLevelFolderId").as[String])))
        .asLongAs(session => !session("nodeIds").as[List[String]].isEmpty) {
          exec(session => {
            logger.debug("nodeIds = " + session("nodeIds").as[List[String]])
            session})
          // Pop nodeId from nodeIds list and get its children nodes
          .exec(session => session.set("nodeId", session("nodeIds").as[List[String]].head))
          .exec(session => {
            logger.debug("Calling GetChildren on nodeId " + session("nodeId").as[String])
            session})          
          .exec(Actions.getChildren("${nodeId}").asJSON
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
