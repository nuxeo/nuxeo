package org.nuxeo.drive.bench

import io.gatling.core.Predef._

object ServerFeeder {

  val run = (thinkTime: Integer) => {
    val filename = "file_${user}"
    val halfSleep = (thinkTime.floatValue() / 2).ceil.toInt
    group("Server Feeder") {
      feed(Feeders.users)
        .exec(
          Actions.createFileDocument(Const.workspacePath + "/" + Const.commonFolder, filename)
        ).pause(halfSleep)
        .exec(
          Actions.createFileDocument(Const.workspacePath + "/" + Const.userFolder, filename)
        ).pause(halfSleep)
        .repeat(2, "count") {
        exec(
          Actions.updateFileDocument(Const.workspacePath + "/" + Const.commonFolder, filename)
        ).pause(halfSleep)
          .exec(
            Actions.updateFileDocument(Const.workspacePath + "/" + Const.userFolder, filename)
          ).pause(halfSleep)
      }.exec(
          Actions.deleteFileDocument(Const.workspacePath + "/" + Const.commonFolder + "/" + filename)
        ).pause(halfSleep)
        .exec(
          Actions.deleteFileDocument(Const.workspacePath + "/" + Const.userFolder + "/" + filename)
        )
    }
  }

}
