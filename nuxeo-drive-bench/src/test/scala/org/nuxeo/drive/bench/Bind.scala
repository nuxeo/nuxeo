package org.nuxeo.drive.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.nuxeo.drive.bench.Actions._

object Bind {

  val run = (thinkTime: Integer) => {
    group("Bind") {
      feed(Feeders.users)
        .feed(Feeders.deviceId)
        .exec(
          http("Check drive support")
            .get("/site/automation")
            .headers(Headers.default).header("X-Devince-Id", "${deviceId}")
            .header("X-user-Id", "${user}")
            .basicAuth("${user}", "${password}")
            .check(status.is(200)).check(regex("GetTopLevelFolder").exists))
        .exec(
          http("Bind drive")
            .get("/authentication/token")
            .headers(Headers.default).header("X-Devince-Id", "${deviceId}")
            .header("X-user-Id", "${user}")
            .basicAuth("${user}", "${password}")
            .queryParamSeq(Seq(
            ("applicationName", "Nuxeo Drive"),
            ("deviceDescription", "Gatling Test"),
            ("revoke", "false"),
            ("deviceId", "${deviceId}"),
            ("permission", "ReadWrite")))
            .check(status.in(200 to 201)).check(bodyString.saveAs("token")))
        .doIf(session => saveToken(session("token").as[String], session("user").as[String], session("deviceId")
        .as[String])) {
        exec()
      }
        .exec(
          http("Token auth")
            .get("/site/automation/NuxeoDrive.GetTopLevelFolder")
            .headers(Headers.default).header("X-Devince-Id", "${deviceId}")
            .header("X-user-Id", "${user}")
            .header("X-Authentication-Token", "${token}")
            .header("Content-Type", "application/json+nxrequest")
            .body(StringBody( """{"params":{}}"""))
            .check(status.in(200))) //.check(regex("canCreateChild").exists))
        .pause(thinkTime)
    }

  }

}
