package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object BatchedRemoteScan {

  val logger = Logger(LoggerFactory.getLogger(getClass))

  /**
   * Let's simultate Nuxeo Drive batched remote scan.
   */
  def run = (batchSize: Integer) => {
    scenario("BatchedRemoteScan")
    .group("BatchedRemoteScan") {
      feed(Feeders.token)
        .exec(Actions.fetchAutomationAPI())
        // Get top level folder
        .exec(Actions.getTopLevelFolder().asJSON
          .check(jsonPath("$.id").saveAs("topLevelFolderId"))
          .check(jsonPath("$.canGetDescendants").ofType[Boolean].saveAs("canGetDescendants")))
        .exec(Actions.getFileSystemItem("${topLevelFolderId}"))
        // Initial call to GetChangeSummary
        .exec(Actions.getChangeSummary(None, None).asJSON
          .check(jsonPath("$.upperBound").saveAs("upperBound"))
          .check(jsonPath("$.activeSynchronizationRootDefinitions").saveAs("roots")))
        // Calls to GetChildren or GetDescendants depending on the node type
        .exec(session => session.set("nodes", List(Map("id" -> session("topLevelFolderId").as[String],
          "canGetDescendants" -> session("canGetDescendants").as[Boolean]))))
        .asLongAs(session => !session("nodes").as[List[Map[String, Any]]].isEmpty) {
          exec(session => {
            logger.debug("nodes = " + session("nodes").as[List[Map[String, Any]]])
            session})
          // Pop node from nodes list
          .exec(session => session.set("nodeId", session("nodes").as[List[Map[String, Any]]].head("id"))
            .set("canGetDescendants", session("nodes").as[List[Map[String, Any]]].head("canGetDescendants")))
          .doIfOrElse(session => session("canGetDescendants").as[Boolean]) {
            // Get node descendants by batch
            exec(session => session.set("lowerId", ""))
            .asLongAs(session => !session("lowerId").asOption[String].isEmpty) {
              exec(session => {
                logger.debug("Calling GetDescendants on nodeId " + session("nodeId").as[String] + ", with batch size " + batchSize + ", starting from id " + session("lowerId").as[String])
                session})
              .exec(
                Actions.getDescendants("${nodeId}", batchSize.toString, "${lowerId}").asJSON
                  .check(jsonPath("$[*]").ofType[Map[String, Any]].findAll
                    .transformOption(extract => extract match {
                      case None => Some(Vector.empty)
                      case descendants => descendants
                    }).saveAs("descendants")))
                .doIfOrElse(session => session("descendants").as[Vector[Map[String, Any]]].isEmpty) {
                  exec(session => session.remove("lowerId"))
                } {
                  exec(session => session.set("lowerId", session("descendants").as[Vector[Map[String, Any]]].last("id")))
                }
            }
            .exec(session => session.set("nodes", session("nodes").as[List[Map[String, Any]]].tail))
          } {
            // Get node children
            exec(session => {
              logger.debug("Calling GetChildren on nodeId " + session("nodeId").as[String])
              session})
            .exec(Actions.getChildren("${nodeId}").asJSON
              .check(jsonPath("$[*]").ofType[Map[String, Any]].findAll
                .transformOption(extract => extract match {
                  case None => Some(Vector.empty)
                  case children => children
                }).saveAs("children")))
            // Add foldersih children nodes to nodes list
            .exec(session => session.set("nodes",
              session("children").as[Vector[Map[String, Any]]].filter(_("folder").asInstanceOf[Boolean]).map(_.filterKeys(Set("id", "canGetDescendants")))
              ++ session("nodes").as[List[Map[String, Any]]].tail))
          }
        }
    }
  }
}
