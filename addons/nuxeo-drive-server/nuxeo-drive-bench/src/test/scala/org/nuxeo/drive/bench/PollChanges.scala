package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object PollChanges {

  def run = (thinkTime: Integer) => {
    group("Poll") {
      feed(Feeders.token)
        .exec(Actions.fetchAutomationAPI())
        .exec(Actions.getClientUpdateInfo())
        .exec(Actions.getTopLevelFolder().asJSON.check(jsonPath("$.id").saveAs("topLevelFolderId")))
        .exec(Actions.getFileSystemItem("${topLevelFolderId}"))
        .exec(Actions.getChangeSummary(None, None).asJSON
          .check(jsonPath("$.upperBound").saveAs("upperBound"))
          .check(jsonPath("$.activeSynchronizationRootDefinitions").saveAs("roots"))
        ).pause(thinkTime)
        .repeat(100, "count") {
          exec(Actions.getChangeSummary(Some("${upperBound}"), Some("${roots}")).asJSON
            .check(jsonPath("$.upperBound").saveAs("upperBound"))
          ).pause(thinkTime)
        }
    }
  }
}
