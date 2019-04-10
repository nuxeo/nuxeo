package org.nuxeo.drive.bench

import com.redis.RedisClientPool

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.util.Random

object Redis {
  val pool = new RedisClientPool("localhost", 6379)
  val key = "nxdrive-token"
}

object Actions {

  def encode = (path: String) => {
    java.net.URLEncoder.encode(path, "UTF-8")
  }

  def saveToken(token: String, user: String, device: String) = {
    Redis.pool.withClient(
      body = client => {
        val value: String = user + "|" + token + "|" + device
        client.sadd(Redis.key, value)
      })
    false
  }

  def createDocumentIfNotExists = (parent: String, name: String, docType: String) => {
    exitBlockOnFail {
      exec(
        http("Check if document exists")
          .head(Constants.API_PATH + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .check(status.in(404)))
        .exec(
          http("Create " + docType)
            .post(Constants.API_PATH + parent)
            .headers(Headers.base)
            .header("Content-Type", "application/json")
            .basicAuth("${user}", "${password}")
            .body(StringBody(
            """{ "entity-type": "document", "name":"""" + name + """", "type": """" + docType +
              """","properties": {"dc:title":"""" + name + """", "dc:description": "Gatling bench folder"}}""".stripMargin))
            .check(status.in(201)))
    }
  }

  def createDocumentIfNotExistsAsAdmin = (parent: String, name: String, docType: String) => {
    exitBlockOnFail {
      exec(
        http("Check if document exists")
          .head(Constants.API_PATH + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${adminId}", "${adminPassword}")
          .check(status.in(404)))
        .exec(
          http("Create " + docType + " as admin")
            .post(Constants.API_PATH + parent)
            .headers(Headers.base)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
            """{ "entity-type": "document", "name":"""" + name + """", "type": """" + docType +
              """","properties": {"dc:title":"""" + name + """", "dc:description": "Gatling bench folder"}}""".stripMargin))
            .check(status.in(201)))
    }
  }

  def createFileDocument = (parent: String, name: String) => {
    val filename = name + ".txt"
    exec(
      http("Initialize upload batch")
        .post("/api/v1/upload")
        .headers(Headers.base)
        .basicAuth("${user}", "${password}")
        .asJSON.check(jsonPath("$.batchId").saveAs("batchId"))
    ).exec(
      http("Create server file Upload")
        .post("/api/v1/upload/${batchId}/0")
        .headers(Headers.base)
        .header("X-File-Name", filename)
        .basicAuth("${user}", "${password}")
        .body(StringBody("You know content file"))
    ).exec(
        http("Create server File")
          .post(Constants.API_PATH + parent)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .body(StringBody(
          """{ "entity-type": "document", "name":"""" + name + """", "type": "File","properties": {"dc:title":"""" +
            name +
            """", "dc:description": "Gatling bench file", "file:content": {"upload-batch":"${batchId}"""" +
            ""","upload-fileId":"0"}}}""".stripMargin))
          .check(status.in(201)))
  }

  def updateFileDocument = (parent: String, name: String) => {
    val filename = name + "txt"
    exec(
      http("Initialize upload batch")
        .post("/api/v1/upload")
        .headers(Headers.base)
        .basicAuth("${user}", "${password}")
         .asJSON.check(jsonPath("$.batchId").saveAs("batchId"))
    ).exec(
      http("Update server file Upload")
        .post("/api/v1/upload/${batchId}/0")
        .headers(Headers.base)
        .header("X-File-Name", filename)
        .basicAuth("${user}", "${password}")
        .body(StringBody("You know content file " + Random.alphanumeric.take(2)))
    ).exec(
        http("Update server File")
          .put(Constants.API_PATH + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .body(StringBody(
          """{ "entity-type": "document", "name":"""" + name + """", "type": "File","properties": {"file:content": {"upload-batch":"${batchId}"""" +
            ""","upload-fileId":"0"}}}""".stripMargin))
          .check(status.in(200)))
  }

  def deleteFileDocument = (path: String) => {
    http("Delete server File")
      .delete(Constants.API_PATH + path)
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${user}", "${password}")
      .check(status.in(204))
  }

  def deleteFileDocumentAsAdmin = (path: String) => {
    http("Delete server File")
      .delete(Constants.API_PATH + path)
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .check(status.in(204))
  }

  def createUserIfNotExists = (groupName: String) => {
    exitBlockOnFail {
      exec(
        http("Check if user exists")
          .head("/api/v1/user/${user}")
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${adminId}", "${adminPassword}")
          .check(status.in(404)))
        .exec(
          http("Create user")
            .post("/api/v1/user")
            .headers(Headers.nxProperties)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
            """{"entity-type":"user","id":"${user}","properties":{"firstName":null,"lastName":null,"password":"${password}","groups":["""" +
              groupName + """"],"company":null,"email":"devnull@nuxeo.com","username":"${user}"},"extendedGroups":[{"name":"members","label":"Members group","url":"group/members"}],"isAdministrator":false,"isAnonymous":false}"""))
            .check(status.in(201)))
    }
  }

  def deleteUser = () => {
    http("Delete user")
      .delete("/api/v1/user/${user}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .check(status.in(204))
  }

  def createGroupIfNotExists = (groupName: String) => {
    exitBlockOnFail {
      exec(
        http("Check if group exists")
          .head("/api/v1/group/" + groupName)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${adminId}", "${adminPassword}")
          .check(status.in(404)))
        .exec(
          http("Create group")
            .post("/api/v1/group")
            .headers(Headers.nxProperties)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
            """{"entity-type":"group","groupname":"""" + groupName + """", "groupLabel": "Gatling group"}"""))
            .check(status.in(201)))
    }
  }

  def deleteGroup = (groupName: String) => {
    http("Delete user")
      .delete("/api/v1/group/" + groupName)
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .check(status.in(204))
  }

  def synchronyzeFolder = (path: String) => {
    http("Synchronyze a folder")
      .post(Constants.API_PATH + path + "/@op/NuxeoDrive.SetSynchronization")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${user}", "${password}")
      .body(StringBody("""{"params":{"enable": "true"}}""".stripMargin))
      .check(status.in(200))
  }

  def unSynchronyzeFolder = (path: String) => {
    http("Synchronyze a folder")
      .post(Constants.API_PATH + path + "/@op/NuxeoDrive.SetSynchronization")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${user}", "${password}")
      .body(StringBody("""{"params":{"enable": "false"}}""".stripMargin))
      .check(status.in(200))
  }

  def getDriveToken = () => {
    exec(
      http("Get drive token")
        .get("/authentication/token")
        .headers(Headers.base)
        .headers(Headers.drive)
        .header("X-Device-Id", "${deviceId}")
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
  }

  def grantReadWritePermission = (path: String, principal: String) => {
    http("Grant write permission")
      .post(Constants.API_PATH + path + "/@op/Document.SetACE")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .body(StringBody("""{"params":{"permission": "ReadWrite", "user": """" + principal + """"}}""".stripMargin))
      .check(status.in(200))
  }

  def fetchAutomationAPI = () => {
    http("Hello Automation")
      .get(Constants.AUTOMATION_PATH + "/")
      .headers(Headers.base)
      .headers(Headers.drive)
      .header("X-Device-Id", "${deviceId}")
      .header("X-user-Id", "${user}")
      .header("X-Authentication-Token", "${token}")
      .check(status.in(200))
      .check(regex("NuxeoDrive.GetClientUpdateInfo").exists)
      .check(regex("NuxeoDrive.GetTopLevelFolder").exists)
      .check(regex("NuxeoDrive.GetFileSystemItem").exists)
      .check(regex("NuxeoDrive.GetChangeSummary").exists)
      .check(regex("NuxeoDrive.GetChildren").exists)
  }

  def getClientUpdateInfo = () => {
    http("Get client update info")
      .post(Constants.AUTOMATION_PATH + "/NuxeoDrive.GetClientUpdateInfo")
      .headers(Headers.nxProperties)
      .headers(Headers.drive)
      .header("X-Device-Id", "${deviceId}")
      .header("X-user-Id", "${user}")
      .header("X-Authentication-Token", "${token}")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"params":{}}"""))
      .check(status.in(200)).check(regex("serverVersion").exists)
  }

  def getTopLevelFolder = () => {
    http("Get top level folder")
      .post(Constants.AUTOMATION_PATH + "/NuxeoDrive.GetTopLevelFolder")
      .headers(Headers.nxProperties)
      .headers(Headers.drive)
      .header("X-Device-Id", "${deviceId}")
      .header("X-user-Id", "${user}")
      .header("X-Authentication-Token", "${token}")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"params":{}}"""))
      .check(status.in(200)).check(regex("canCreateChild").exists)
  }

  def getFileSystemItem = (id: String) => {
    http("Get file system item")
      .post(Constants.AUTOMATION_PATH + "/NuxeoDrive.GetFileSystemItem")
      .headers(Headers.nxProperties)
      .headers(Headers.drive)
      .header("X-Device-Id", "${deviceId}")
      .header("X-user-Id", "${user}")
      .header("X-Authentication-Token", "${token}")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"params": {"id": """" + id + """"}}""".stripMargin))
      .check(status.in(200))
  }

  def getChangeSummary = (lowerBound: Option[String], lastSyncActiveRootDefinitions: Option[String]) => {
    val params = new StringBuilder
    params += '{'
    var isLowerBound = false
    if (!lowerBound.isEmpty) {
      isLowerBound = true
      params ++= """"lowerBound": """
      params ++= lowerBound.get
    }
    if (!lastSyncActiveRootDefinitions.isEmpty) {
      if (isLowerBound) {
        params ++= ", "
      }
      params ++= """"lastSyncActiveRootDefinitions": """"
      params ++= lastSyncActiveRootDefinitions.get
      params += '"'
    }
    params += '}'
    http("Get change summary")
      .post(Constants.AUTOMATION_PATH + "/NuxeoDrive.GetChangeSummary")
      .headers(Headers.nxProperties)
      .headers(Headers.drive)
      .header("X-Device-Id", "${deviceId}")
      .header("X-user-Id", "${user}")
      .header("X-Authentication-Token", "${token}")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"params": """ + params.toString + "}".stripMargin))
      .check(status.in(200))
  }

  def getChildren = (id: String) => {
    http("Get children")
      .post(Constants.AUTOMATION_PATH + "/NuxeoDrive.GetChildren")
      .headers(Headers.nxProperties)
      .headers(Headers.drive)
      .header("X-Device-Id", "${deviceId}")
      .header("X-user-Id", "${user}")
      .header("X-Authentication-Token", "${token}")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"params": {"id": """" + id + """"}}""".stripMargin))
      .check(status.in(200))
  }

  def scrollDescendants = (id: String, scrollId: String, batchSize: String) => {
    val params = new StringBuilder
    params ++= """{"id": """"
    params ++= id
    params ++= """", "batchSize": """
    params ++= batchSize
    if (!scrollId.isEmpty) {
      params ++= """, "scrollId": """"
      params ++= scrollId.get
      params += '"'
    }
    params += '}'
    http("Scroll descendants")
      .post(Constants.AUTOMATION_PATH + "/NuxeoDrive.ScrollDescendants")
      .headers(Headers.nxProperties)
      .headers(Headers.drive)
      .header("X-Device-Id", "${deviceId}")
      .header("X-user-Id", "${user}")
      .header("X-Authentication-Token", "${token}")
      .header("Content-Type", "application/json")
      .body(StringBody("""{"params": """ + params.toString + "}".stripMargin))
      .check(status.in(200))
  }

}

object Feeders {
  def clearTokens() = {
    Redis.pool.withClient(
      body = client => {
        client.del(Redis.key)
      })
  }

  def getToken() = {
    var map = Map[String, String]()
    Redis.pool.withClient(
      body = client => {
        val value = client.srandmember(Redis.key).get
        val values = value.split("\\|")
        map += "user" -> values(0)
        map += "token" -> values(1)
        map += "deviceId" -> values(2)
      })
    // println("return " + map)
    map
  }

  // val admin = Iterator.continually(Map("adminId" -> "Administrator", "adminPassword" -> "Administrator))
  val deviceId = Iterator.continually(Map("deviceId" -> ("cafebabe-cafe-babe-cafe-" + Random.alphanumeric.take(13)
    .mkString)))
  val token = Iterator.continually(getToken())
  val usersQueue = csv("users.csv").queue
  val users = csv("users.csv").circular
  val admins = csv("admins.csv").circular

}


