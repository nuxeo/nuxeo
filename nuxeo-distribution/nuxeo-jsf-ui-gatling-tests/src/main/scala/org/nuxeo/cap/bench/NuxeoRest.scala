package org.nuxeo.cap.bench

/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Delbosc Benoit
 */

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.util.Random

object NuxeoRest {

  def encodePath = (path: String) => {
    java.net.URLEncoder.encode(path, "UTF-8")
  }

  /** Create a document ins the gatling workspace  */
  def createDocument() = {
    exec()
      .doIf("${blobPath.isUndefined()}") {
        exec(
          http("Create ${type}")
            .post(Constants.GAT_API_PATH + "/${parentPath}")
            .headers(Headers.base)
            .header("Content-Type", "application/json")
            .basicAuth("${user}", "${password}")
            .body(StringBody("${payload}"))
            .check(status.saveAs("status"))
            .check(status.is(201))
        )
      }.doIf("${blobPath.exists()}") {
      exec(
        http("Upload Get a batch id")
          .post("/api/v1/upload")
          .headers(Headers.base)
          .basicAuth("${user}", "${password}")
          .asJSON.check(jsonPath("$.batchId").saveAs("batchId"))
      ).exec(
        http("Upload File ${type}")
          .post("/api/v1/upload/${batchId}/0")
          .headers(Headers.base)
          .header("X-File-Name", "${blobFilename}")
          .header("Content-Type", "${blobMimeType}")
          .basicAuth("${user}", "${password}")
          .body(RawFileBody("${blobPath}"))
      ).exec(
        http("Upload Create ${type} document")
          .post(Constants.GAT_API_PATH + "/${parentPath}")
          .headers(Headers.base)
          .header("Content-Type", "application/json")
          .basicAuth("${user}", "${password}")
          .body(StringBody(session => session("payload").as[String].replaceAll("_BATCH_ID_", session("batchId").as[String])))
          .check(status.saveAs("status"))
          .check(status.in(201))
      )
    }
  }

  /** Create a document that may already exists, in this case Nuxeo will change the doc name */
  def createAgainDocument() = {
    createDocument()
  }

  /** Update the description of a document in the gatling workspace */
  def updateDocument() = {
    http("Update ${type}")
      .put(Constants.GAT_API_PATH + "/${url}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${user}", "${password}")
      .body(StringBody( """{ "entity-type": "document","properties": {"dc:source":"nxgatudpate_${counterName}"}}"""))
      .check(status.in(200))
  }

  def getDocument(comment: String = "Get ${type}", schemas: String = "*", enrichers: String = "", parts: String = "nxp-19581") = {
    http(comment)
      .get(Constants.GAT_API_PATH + "/${url}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .header("X-NXproperties", schemas)
      .header("X-NXenrichers.document", enrichers)
      .header("X-NXfetch.document", parts)
      .basicAuth("${user}", "${password}")
      .check(status.in(200))
  }

  def downloadBlob(comment: String = "Download ${type} file") = {
    exec()
      .doIf("${blobPath.exists()}") {
        exec(
          http(comment)
//            .get("http://localhost:18080/api/download?url=" + Parameters.getBaseUrl() + Constants.GAT_API_PATH + "/${url}/@blob/blobholder:0")
            .get(Constants.GAT_API_PATH + "/${url}/@blob/file:content")
            .headers(Headers.base)
            .basicAuth("${user}", "${password}")
            .check(status.in(200, 404))
        )
      }
  }

  def directS3DownloadBlob(comment: String = "Download: ") = {
    exec()
      .doIf("${blobPath.exists()}") {
        exec(
            http(comment + "Ask S3 url")
            .post(Constants.GAT_API_PATH + "/${url}/@op/Blob.Get")
            .headers(Headers.base)
            .header("Content-Type", "application/json")
            .basicAuth("${user}", "${password}")
            .body(StringBody("""{"params":{}}"""))
            .disableFollowRedirect
            .check(status.in(302)).check(header("Location").saveAs("s3url"))
        ).exec(
          http(comment + "S3 Download ${type}").get("${s3url}").check(status.in(200))
        )
      }
  }

  def deleteDocument() = {
    http("Delete ${type}")
      .delete(Constants.GAT_API_PATH + "/${url}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${user}", "${password}")
      .check(status.in(204))
  }

  def search(nxql: String, sortBy: String = "", sortOrder: String = "", comment: String = "Search with NXQL", pageSize: Int = 10, maxResults: Int = 100,
             currentPageIndex: Int = 0, schemas: String = "*", enrichers: String = "", parts: String = "nxp-19581") = {
    http(comment)
      .get(Constants.API_QUERY)
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .header("X-NXproperties", schemas)
      .header("X-NXenrichers.document", enrichers)
      .header("X-NXfetch.document", parts)
      .basicAuth("${user}", "${password}")
      .queryParam("query", nxql)
      .queryParam("pageSize", pageSize)
      .queryParam("maxResults", maxResults)
      .queryParam("currentPageIndex", currentPageIndex)
      .queryParam("sortBy", sortBy)
      .queryParam("sortOrder", sortOrder)
      .check(status.in(200))
  }

  def getParentFolderOfCurrentDocument(comment: String = "Get Parent Folder", schemas: String = "*", enrichers: String = "", parts: String = "") = {
    http(comment)
      .get(Constants.GAT_API_PATH + "/${parentPath}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .header("X-NXproperties", schemas)
      .header("X-NXenrichers.document", enrichers)
      .basicAuth("${user}", "${password}")
      .check(status.in(200))
  }

  // When status is 200 it already exists, 201 otherwhise
  def createDocumentIfNotExists = (parent: String, name: String, docType: String) => {
    exec(
      http("Check if document exists")
        .head(Constants.API_PATH + parent + "/" + name)
        .headers(Headers.base)
        .header("Content-Type", "application/json")
        .basicAuth("${user}", "${password}")
        .check(status.in(200, 404).saveAs("status")))
      .doIf(session => session("status").as[Integer].equals(404)) {
        exec(
          http("Create " + docType)
            .post(Constants.API_PATH + parent)
            .headers(Headers.base)
            .header("Content-Type", "application/json")
            .basicAuth("${user}", "${password}")
            .body(StringBody(
              """{ "entity-type": "document", "name":"""" + name + """", "type": """" + docType +
                """","properties": {"dc:title":"""" + name +
                """", "dc:description": "Gatling bench """ +
                docType +
                """"}}"""))
            .check(status.in(201).saveAs("status")))
      }
  }

  // When status is 200 it already exists, 201 otherwhise
  def createDocumentIfNotExistsAsAdmin = (parent: String, name: String, docType: String) => {
    exec(
      http("Check if document exists")
        .head(Constants.API_PATH + parent + "/" + name)
        .headers(Headers.base)
        .header("Content-Type", "application/json")
        .basicAuth("${adminId}", "${adminPassword}")
        .check(status.in(200, 404).saveAs("status")))
      .doIf(session => session("status").as[Integer].equals(404)) {
        exec(
          http("Create " + docType + " as admin")
            .post(Constants.API_PATH + parent)
            .headers(Headers.base)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
              """{ "entity-type": "document", "name":"""" + name + """", "type": """" + docType +
                """","properties": {"dc:title":"""" + name +
                """", "dc:description": "Gatling bench folder"}}""".stripMargin))
            .check(status.in(201).saveAs("status")))
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
        .post("/api/v1/upload/{batchId}/0")
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

  // When status is 200 it already exists, 201 otherwhise
  def createUserIfNotExists = (groupName: String) => {
    exec(
      http("Check if user exists")
        .head("/api/v1/user/${user}")
        .headers(Headers.base)
        .header("Content-Type", "application/json")
        .basicAuth("${adminId}", "${adminPassword}")
        .check(status.in(200, 404).saveAs("status")))
      .doIf(session => session("status").as[Integer].equals(404)) {
        exec(
          http("Create user")
            .post("/api/v1/user")
            .headers(Headers.default)
            .header("Content-Type", "application/json")
            .basicAuth("${adminId}", "${adminPassword}")
            .body(StringBody(
              """{"entity-type":"user","id":"${user}","properties":{"firstName":null,"lastName":null,"password":"${password}","groups":["""" +
                groupName +
                """"],"company":null,"email":"devnull@nuxeo.com","username":"${user}"},"extendedGroups":[{"name":"members","label":"Members group","url":"group/members"}],"isAdministrator":false,"isAnonymous":false}"""))
            .check(status.in(201).saveAs("status")))
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

  // When status is 200 it already exists, 201 otherwhise
  def createGroupIfNotExists = (groupName: String) => {
    exec(
      http("Check if group exists")
        .head("/api/v1/group/" + groupName)
        .headers(Headers.base)
        .header("Content-Type", "application/json")
        .basicAuth("${adminId}", "${adminPassword}")
        .check(status.in(200, 404).saveAs("status")))
      .doIf(session => session("status").as[Integer].equals(404)) {
        exec(http("Create group")
          .post("/api/v1/group")
          .headers(Headers.default)
          .header("Content-Type", "application/json")
          .basicAuth("${adminId}", "${adminPassword}")
          .body(StringBody(
            """{"entity-type":"group","groupname":"""" + groupName + """", "groupLabel": "Gatling group"}"""))
          .check(status.in(201).saveAs("status")))
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


  def grantReadWritePermission = (path: String, principal: String) => {
    http("Grant write permission")
      .post(Constants.API_PATH + path + "/@op/Document.SetACE")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
      .body(StringBody( """{"params":{"permission": "ReadWrite", "user": """" + principal + """"}}""".stripMargin))
      .check(status.in(200))
  }

  def waitForAsyncJobs = () => {
    http("Wait For Async")
      .post(Constants.AUTOMATION_PATH + "/Elasticsearch.WaitForIndexing")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("content-type", "application/json+nxrequest")
      .body(StringBody( """{"params":{"timeoutSecond": "3600", "refresh": "true", "waitForAudit": "true"},"context":{}}"""))
  }

  def reindexAll = () => {
    exitBlockOnFail {
      exec(
        http("Reindex All repository")
          .post(Constants.AUTOMATION_PATH + "/Elasticsearch.Index")
          .basicAuth("${adminId}", "${adminPassword}")
          .headers(Headers.base)
          .header("content-type", "application/json+nxrequest")
          .body(StringBody( """{"params":{},"context":{}}"""))
      ).exec(waitForAsyncJobs())
    }

  }

}
