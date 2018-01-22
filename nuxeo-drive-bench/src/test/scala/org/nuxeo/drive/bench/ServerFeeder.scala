package org.nuxeo.drive.bench

import io.gatling.core.Predef._

object ServerFeeder {

  def run = (thinkTime: Integer) => {
    val filename = "file_${user}"
    val halfSleep = (thinkTime.floatValue() / 2).ceil.toInt
    group("Server Feeder") {
      feed(Feeders.users)
        .exec(
          Actions.createFileDocument("/default-domain/002 Donnees communes/3 Admin/3 KBis/NX-tests/" + Constants.GAT_FOLDER_NAME, filename)
        ).pause(halfSleep)
        .exec(
          Actions.createFileDocument("/default-domain/002 Donnees communes/3 Admin/3 KBis/NX-tests/" + Constants.GAT_USER_FOLDER_NAME, filename)
        ).pause(halfSleep)
        .repeat(2, "count") {
        exec(
          Actions.updateFileDocument("/default-domain/002 Donnees communes/3 Admin/3 KBis/NX-tests/" + Constants.GAT_FOLDER_NAME, filename)
        ).pause(halfSleep)
          .exec(
            Actions.updateFileDocument("/default-domain/002 Donnees communes/3 Admin/3 KBis/NX-tests/" + Constants.GAT_USER_FOLDER_NAME, filename)
          ).pause(halfSleep)
      }.exec(
          Actions.deleteFileDocument("/default-domain/002 Donnees communes/3 Admin/3 KBis/NX-tests/" + Constants.GAT_FOLDER_NAME + "/" + filename)
        ).pause(halfSleep)
        .exec(
          Actions.deleteFileDocument("/default-domain/002 Donnees communes/3 Admin/3 KBis/NX-tests/" + Constants.GAT_USER_FOLDER_NAME + "/" + filename)
        )
    }
  }

}
