package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object PollChanges {

  val run = (thinkTime: Integer) => {
    group("Poll") {
      feed(Feeders.token)
        .exec(
          http("Hello automation")
            .get("/site/automation/")
            .headers(Headers.default).header("X-Devince-Id", "${deviceId}")
            .header("X-user-Id", "${user}")
            .header("X-Authentication-Token", "${token}")
            .check(status.in(200))
            .check(regex("NuxeoDrive.GetTopLevelFolder").exists)
        )
        .exec(
          http("Get client update info")
            .post("/site/automation/NuxeoDrive.GetClientUpdateInfo")
            .headers(Headers.default).header("X-Devince-Id", "${deviceId}")
            .header("X-user-Id", "${user}")
            .header("X-Authentication-Token", "${token}")
            .header("Content-Type", "application/json+nxrequest")
            .body(StringBody( """{"params":{}}"""))
            .check(status.in(200)).check(regex("serverVersion").exists)
        )
        .exec(
          http("Get top level folders")
            .post("/site/automation/NuxeoDrive.GetTopLevelFolder")
            .headers(Headers.default).header("X-Devince-Id", "${deviceId}")
            .header("X-user-Id", "${user}")
            .header("X-Authentication-Token", "${token}")
            .header("Content-Type", "application/json+nxrequest")
            .body(StringBody( """{"params":{}}"""))
            .check(status.in(200)).check(regex("canCreateChild").exists)
        )
        .exec(
          http("Get file system item toplevel")
            .post("/site/automation/NuxeoDrive.GetFileSystemItem")
            .headers(Headers.default).header("X-Devince-Id", "${deviceId}")
            .header("X-user-Id", "${user}")
            .header("X-Authentication-Token", "${token}")
            .header("Content-Type", "application/json+nxrequest")
            .body(StringBody( """{"params": {"id": "org.nuxeo.drive.service.impl.DefaultTopLevelFolderItemFactory#"}}"""))
            .check(status.in(200))
        )
        .exec(
          http("Get change summary")
            .post("/site/automation/NuxeoDrive.GetChangeSummary")
            .headers(Headers.default).header("X-Devince-Id", "${deviceId}")
            .header("X-user-Id", "${user}")
            .header("X-Authentication-Token", "${token}")
            .header("Content-Type", "application/json+nxrequest")
            .body(StringBody( """{"params": {}}"""))
            .check(status.in(200)).asJSON.check(jsonPath("$.upperBound").saveAs("upperBound"))
            .check(jsonPath("$.activeSynchronizationRootDefinitions").saveAs("roots"))
        ).pause(thinkTime)
        .repeat(100, "count") {
        exec(
          http("Get change summary")
            .post("/site/automation/NuxeoDrive.GetChangeSummary")
            .headers(Headers.default).header("X-Devince-Id", "${deviceId}")
            .header("X-user-Id", "${user}")
            .header("X-Authentication-Token", "${token}")
            .header("Content-Type", "application/json+nxrequest")
            .body(StringBody(
            """{"params": {"lowerBound": ${upperBound}, "lastSyncActiveRootDefinitions": "${roots}"}}""".stripMargin)
            ).check(status.in(200))
            .asJSON.check(jsonPath("$.upperBound").saveAs("upperBound"))
        ).pause(thinkTime)
      }
    }
  }
}
