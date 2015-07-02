package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object PollChanges {

  val run = (thinkTime: Integer) => {
    group("Poll") {
      pause(10)
        .repeat(10, "pollCount") {
        feed(Feeders.token)
          .exec(
            http("Token auth")
              .get("/site/automation/NuxeoDrive.GetTopLevelFolder")
              .headers(Headers.default).header("X-Devince-Id", "${deviceId}")
              .header("X-user-Id", "${userId}")
              .header("X-Authentication-Token", "${token}")
              .header("Content-Type", "application/json+nxrequest")
              .body(StringBody( """{"params":{}}"""))
              .check(status.in(200))) //.check(regex("canCreateChild").exists))
          .pause(thinkTime)
      }
    }

  }

}
