package org.nuxeo.drive.bench

import com.redis.RedisClientPool
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.util.Random

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
        http("Exists")
          .head("/api/v1/path/default-domain/workspaces" + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .check(status.in(404)))
        .exec(
          http("Create document " + docType)
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
        http("Exists")
          .head("/api/v1/path/default-domain/workspaces" + parent + "/" + name)
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${adminId}", "${adminPassword}")
          .check(status.in(404)))
        .exec(
          http("Create Document as Admin " + docType)
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

  val createUserIfNotExists = () => {
    exitBlockOnFail {
      exec(
        http("User Exists")
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
            """{"entity-type":"user","id":"${user}","properties":{"firstName":null,"lastName":null,
              |"password":"${password}","groups":["members"],"company":null,"email":"devnull@nuxeo.com","username":"${user}"},
              |"extendedGroups":[{"name":"members","label":"Members group","url":"group/members"}],"isAdministrator":false,"isAnonymous":false}""".stripMargin))
            .check(status.in(201)))
    }
  }

  val synchronyzeFolder = (path: String) => {
    http("Synchronyze folder")
      .post("/api/v1/path/default-domain/workspaces" + path + "/@op/NuxeoDrive.SetSynchronization")
      .headers(Headers.base)
      .header("Content-Type", "application/json+nxrequest")
      .basicAuth("${user}", "${password}")
      .body(StringBody( """{"params":{"enable": "true"}}""".stripMargin))
      .check(status.in(200))
  }

  val bindUser = () => {
    exec(
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
  }

  val grantWrite = (path: String, principal: String) => {
    http("Grant write")
      .post("/api/v1/path/default-domain/workspaces" + path + "/@op/Document.SetACE")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .body(StringBody( """{"params":{"permission": "Write", "user": """" + principal + """"}}""".stripMargin))
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
    println("gettoken")
    var map = Map[String, String]()
    Redis.pool.withClient(
      body = client => {
        val value = client.srandmember(Redis.key).toString
        val values = value.split("\\|")
        map += "user" -> values(0)
        map += "token" -> values(1)
        map += "deviceId" -> values(2)
      })
    println("return " + map)
    map
  }

  // val admin = Iterator.continually(Map("userId" -> "Administrator"))
  val deviceId = Iterator.continually(Map("deviceId" -> ("cafebabe-cafe-babe-cafe-" + Random.alphanumeric.take(13)
    .mkString)))
  val token = Iterator.continually(getToken())
  val usersQueue = csv("users.csv").queue
  val users = csv("users.csv").circular
  val admins = csv("admins.csv").circular
}


