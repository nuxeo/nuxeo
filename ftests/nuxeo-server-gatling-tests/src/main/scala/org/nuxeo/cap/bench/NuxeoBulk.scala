/*
 * (C) Copyright 2018-2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Funsho David
 */
package org.nuxeo.cap.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  * @since 10.2
  */
object NuxeoBulk {

  def bulkUpdateDocument(query: String, property: String, value: String) = {
    http("Update documents")
      .post(Constants.API_SEARCH + "/bulk/setProperties")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("Accept", "application/json")
      .header("content-type", "application/json")
      .queryParam("query", query)
      .body(StringBody( """{"""" + property + """":"""" + value + """"}""")
      )
  }

  def bulkCsvExport(query: String) = {
    http("Export documents in CSV file")
      .post(Constants.API_SEARCH + "/bulk/csvExport")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("Accept", "application/json")
      .header("content-type", "application/json")
      .queryParam("query", query)
  }

  def waitForAction(commandId: String, comment: String = "Wait for action to be completed") = {
    http(comment)
      .post(Constants.AUTOMATION_PATH + "/Bulk.WaitForAction")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("Accept", "application/json")
      .header("content-type", "application/json")
      .check(status.in(200))
      .body(StringBody( """{"params":{"timeoutSecond": "3600", "commandId": "${commandId}"},"context":{}}"""))
  }

  def reindexAll = () => {
    exitBlockOnFail {
      exec(
        http("Submit Reindex")
          .post(Constants.AUTOMATION_PATH + "/Elasticsearch.BulkIndex")
          .basicAuth("${adminId}", "${adminPassword}")
          .headers(Headers.base)
          .header("content-type", "application/json")
          .body(StringBody( """{"params":{},"context":{}}"""))
          .check(status.in(200))
          .check(jsonPath("$.commandId").saveAs("commandId")))
      .exec(waitForAction("${commandId}", "Reindex"))
      .exec(
        http("Get Reindex Status")
          .get(Constants.API_BULK + "/${commandId}")
          .basicAuth("${adminId}", "${adminPassword}")
          .headers(Headers.base)
          .check(jsonPath("$.state").ofType[String].is("COMPLETED"))
          .check(jsonPath("$.total").saveAs("reindexTotal")))
    }
  }

  def versionFullGC = () => {
     exitBlockOnFail {
          exec(
            http("Submit Version Full GC")
              .delete(Constants.API_MANAGEMENT + "/versions/orphaned")
              .basicAuth("${adminId}", "${adminPassword}")
              .headers(Headers.base)
              .check(status.in(200))
              .check(jsonPath("$.commandId").saveAs("commandId")))
         .exec(waitForAction("${commandId}", "Version Full GC"))
         .exec(
            http("Get Version Full GC Status")
              .get(Constants.API_BULK + "/${commandId}")
              .basicAuth("${adminId}", "${adminPassword}")
              .headers(Headers.base)
              .check(jsonPath("$.state").ofType[String].is("COMPLETED"))
              .check(jsonPath("$.total").saveAs("versionsTotal"))
              .check(jsonPath("$.skipCount").saveAs("versionsRetained")))
        }
    }

   def binaryFullGC = () => {
     exitBlockOnFail {
          exec(
            http("Submit Binary Full GC")
              .delete(Constants.API_MANAGEMENT + "/blobs/orphaned")
              .basicAuth("${adminId}", "${adminPassword}")
              .headers(Headers.base)
              .check(status.in(200))
              .check(jsonPath("$.commandId").saveAs("commandId")))
         .exec(waitForAction("${commandId}", "Binary Full GC"))
         .exec(
            http("Get Binary Full GC Status")
              .get(Constants.API_BULK + "/${commandId}")
              .basicAuth("${adminId}", "${adminPassword}")
              .headers(Headers.base)
              .check(jsonPath("$.state").ofType[String].is("COMPLETED"))
              .check(jsonPath("$.total").saveAs("binariesTotal"))
              .check(jsonPath("$.skipCount").saveAs("binariesRetained")))
        }
    }

    def disableScheduler = () => {
      http("Disable scheduler")
        .put(Constants.API_MANAGEMENT + "/scheduler/stop")
        .basicAuth("${adminId}", "${adminPassword}")
        .headers(Headers.base)
        .check(status.in(204))
    }

}
