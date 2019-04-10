package org.nuxeo.drive.bench

import io.gatling.core.Predef._

object ServerFeeder {

  def run = (thinkTime: Integer) => {
    val filename = "file_${user}"
    val halfSleep = (thinkTime.floatValue() / 2).ceil.toInt
    group("Server Feeder") {
      feed(Feeders.users)
        .exec(
          Actions.createFileDocument(Constants.GAT_WS_PATH + "/" + Constants.GAT_FOLDER_NAME, filename)
        ).pause(halfSleep)
        .exec(
          Actions.createFileDocument(Constants.GAT_WS_PATH + "/" + Constants.GAT_USER_FOLDER_NAME, filename)
        ).pause(halfSleep)
        .repeat(2, "count") {
        exec(
          Actions.updateFileDocument(Constants.GAT_WS_PATH + "/" + Constants.GAT_FOLDER_NAME, filename)
        ).pause(halfSleep)
          .exec(
            Actions.updateFileDocument(Constants.GAT_WS_PATH + "/" + Constants.GAT_USER_FOLDER_NAME, filename)
          ).pause(halfSleep)
      }.exec(
          Actions.deleteFileDocument(Constants.GAT_WS_PATH + "/" + Constants.GAT_FOLDER_NAME + "/" + filename)
        ).pause(halfSleep)
        .exec(
          Actions.deleteFileDocument(Constants.GAT_WS_PATH + "/" + Constants.GAT_USER_FOLDER_NAME + "/" + filename)
        )
    }
  }

}
