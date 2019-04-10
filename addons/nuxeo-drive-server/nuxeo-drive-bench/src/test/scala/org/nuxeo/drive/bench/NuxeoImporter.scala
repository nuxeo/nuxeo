package org.nuxeo.drive.bench

/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Delbosc Benoit
 */

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object NuxeoImporter {

  val API_PATH = "/site/randomImporter"

  def massImport(nbThreads: Integer, nbNodes: Integer) = {
    http("Mass import " + nbNodes.toString() + " nodes with " + nbThreads.toString() + " threads")
      .get(API_PATH + "/run")
      .queryParam("targetPath", Constants.GAT_WS_PATH)
      .queryParam("batchSize", "50")
      .queryParam("nbThreads", nbThreads)
      .queryParam("interactive", "true")
      .queryParam("fileSizeKB", "1")
      .queryParam("nbNodes", nbNodes)
      .queryParam("nonUniform", true)
      .queryParam("transactionTimeout", "3600")
      .headers(Headers.base)
      .basicAuth("${adminId}", "${adminPassword}")
  }

  def waitForAsyncJobsAndESIndexation = () => {
    http("Wait For Async and ES Indexation")
      .post(Constants.AUTOMATION_PATH + "/Elasticsearch.WaitForIndexing")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("content-type", "application/json")
      .body(StringBody( """{"params":{"timeoutSecond": "3600", "refresh": "true"},"context":{}}"""))
  }

}
