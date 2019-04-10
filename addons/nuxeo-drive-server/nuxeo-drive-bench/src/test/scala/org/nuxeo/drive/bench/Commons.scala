package org.nuxeo.drive.bench

import com.redis.RedisClientPool
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.util.Random

object Const {
  val workspaceName = "Bench_Drive"
  val workspacePath = "/Bench_Drive"
  val commonFolder = "Common"
  val userFolder = "Folder_${user}"
  val groupName = "gatling"
}

object Headers {

  val base = Map(
    "X-Application-Name" -> "Nuxeo Drive",
    "X-Client-Version" -> "2.0.0625"
  )

  val default = base.++(
    Map(
      "X-Application-Name" -> "Nuxeo Drive",
      "X-Client-Version" -> "2.0.0625",
      "X-Nxdocumentproperties" -> "*",
      "X-Nxproperties" -> "*"
    ))

  val withEnricher = default.++(
    Map(
      "X-NXenrichers.document" -> "files",
      "depth" -> "max"))

}

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

  val createDocumentIfNotExists = (parent: String, name: String, docType: String) => {
    exitBlockOnFail {
      exec(
        http("Check if document exists")
          .head("/api/v1/path/default-domain/workspaces" + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .check(status.in(404)))
        .exec(
          http("Create " + docType)
            .post("/api/v1/path/default-domain/workspaces" + parent)
            .headers(Headers.base)
            .header("Content-Type", "application/json")
            .basicAuth("${user}", "${password}")
            .body(StringBody(
            """{ "entity-type": "document", "name":"""" + name + """", "type": """" + docType +
              """","properties": {"dc:title":"""" + name + """", "dc:description": "Gatling bench folder"}}""".stripMargin))
            .check(status.in(201)))
    }
  }

  val createDocumentIfNotExistsAsAdmin = (parent: String, name: String, docType: String) => {
    exitBlockOnFail {
      exec(
        http("Check if document exists")
          .head("/api/v1/path/default-domain/workspaces" + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${adminId}", "${adminPassword}")
          .check(status.in(404)))
        .exec(
          http("Create " + docType + " as admin")
            .post("/api/v1/path/default-domain/workspaces" + parent)
            .headers(Headers.base)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
            """{ "entity-type": "document", "name":"""" + name + """", "type": """" + docType +
              """","properties": {"dc:title":"""" + name + """", "dc:description": "Gatling bench folder"}}""".stripMargin))
            .check(status.in(201)))
    }
  }

  val createFileDocument = (parent: String, name: String) => {
    val batchId = name
    val filename = name + ".txt"
    exec(
      http("Create server file Upload")
        .post("/api/v1/automation/batch/upload")
        .headers(Headers.base)
        .header("X-Batch-Id", batchId)
        .header("X-File-Idx", "0")
        .header("X-File-Name", filename)
        .basicAuth("${user}", "${password}")
        .body(StringBody("You know content file"))
    ).exec(
        http("Create server File")
          .post("/api/v1/path/default-domain/workspaces" + parent)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .body(StringBody(
          """{ "entity-type": "document", "name":"""" + name + """", "type": "File","properties": {"dc:title":"""" +
            name +
            """", "dc:description": "Gatling bench file", "file:content": {"upload-batch":"""" + batchId +
            """","upload-fileId":"0"}}}""".stripMargin))
          .check(status.in(201)))
  }

  val updateFileDocument = (parent: String, name: String) => {
    val batchId = name
    val filename = name + "txt"
    exec(
      http("Update server file Upload")
        .post("/api/v1/automation/batch/upload")
        .headers(Headers.base)
        .header("X-Batch-Id", batchId)
        .header("X-File-Idx", "0")
        .header("X-File-Name", filename)
        .basicAuth("${user}", "${password}")
        .body(StringBody("You know content file " + Random.alphanumeric.take(2)))
    ).exec(
        http("Update server File")
          .put("/api/v1/path/default-domain/workspaces" + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .body(StringBody(
          """{ "entity-type": "document", "name":"""" + name + """", "type": "File","properties": {"file:content": {"upload-batch":"""" + batchId +
            """","upload-fileId":"0"}}}""".stripMargin))
          .check(status.in(200)))
  }

  val deleteFileDocument = (path: String) => {
    http("Delete server File")
      .delete("/api/v1/path/default-domain/workspaces" + path)
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${user}", "${password}")
      .check(status.in(204))
  }

  val deleteFileDocumentAsAdmin = (path: String) => {
    http("Delete server File")
      .delete("/api/v1/path/default-domain/workspaces" + path)
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .check(status.in(204))
  }

  val createUserIfNotExists = (groupName: String) => {
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
            .headers(Headers.default)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
            """{"entity-type":"user","id":"${user}","properties":{"firstName":null,"lastName":null,"password":"${password}","groups":["""" +
              groupName + """"],"company":null,"email":"devnull@nuxeo.com","username":"${user}"},"extendedGroups":[{"name":"members","label":"Members group","url":"group/members"}],"isAdministrator":false,"isAnonymous":false}"""))
            .check(status.in(201)))
    }
  }

  val deleteUser = () => {
    http("Delete user")
      .delete("/api/v1/user/${user}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .check(status.in(204))
  }

  val createGroupIfNotExists = (groupName: String) => {
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
            .headers(Headers.default)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
            """{"entity-type":"group","groupname":"""" + groupName + """", "groupLabel": "Gatling group"}"""))
            .check(status.in(201)))
    }
  }

  val deleteGroup = (groupName: String) => {
    http("Delete user")
      .delete("/api/v1/group/" + groupName)
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .check(status.in(204))
  }

  val synchronyzeFolder = (path: String) => {
    http("Synchronyze a folder")
      .post("/api/v1/path/default-domain/workspaces" + path + "/@op/NuxeoDrive.SetSynchronization")
      .headers(Headers.base)
      .header("Content-Type", "application/json+nxrequest")
      .basicAuth("${user}", "${password}")
      .body(StringBody( """{"params":{"enable": "true"}}""".stripMargin))
      .check(status.in(200))
  }

  val unSynchronyzeFolder = (path: String) => {
    http("Synchronyze a folder")
      .post("/api/v1/path/default-domain/workspaces" + path + "/@op/NuxeoDrive.SetSynchronization")
      .headers(Headers.base)
      .header("Content-Type", "application/json+nxrequest")
      .basicAuth("${user}", "${password}")
      .body(StringBody( """{"params":{"enable": "false"}}""".stripMargin))
      .check(status.in(200))
  }

  val getDriveToken = () => {
    exec(
      http("Get drive token")
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
  }

  val grantReadWritePermission = (path: String, principal: String) => {
    http("Grant write permission")
      .post("/api/v1/path/default-domain/workspaces" + path + "/@op/Document.SetACE")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .body(StringBody( """{"params":{"permission": "ReadWrite", "user": """" + principal + """"}}""".stripMargin))
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


