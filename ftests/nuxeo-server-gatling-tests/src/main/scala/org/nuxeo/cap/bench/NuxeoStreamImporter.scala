/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and contributors.
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
package org.nuxeo.cap.bench

import io.gatling.core.Predef._
import io.gatling.http.Predef._

object NuxeoStreamImporter {

  def generateDocuments(nbThreads: Integer, nbDocuments: Integer) = {
    http("Generate " + nbDocuments.toString() + " documents using " + nbThreads.toString() + " threads")
      .post(Constants.AUTOMATION_PATH + "/StreamImporter.runRandomDocumentProducers")
      .body(StringBody(s"""{"params":{"nbDocuments":${(nbDocuments.toFloat/nbThreads).toInt},"nbThreads":${nbThreads},"avgBlobSizeKB": "1","transactionTimeout":"3600"}}"""))
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
  }

  def importDocuments(nbThreads: Integer) = {
    http("Mass import with " + nbThreads.toString() + " threads")
      .post(Constants.AUTOMATION_PATH + "/StreamImporter.runDocumentConsumers")
      .body(StringBody(s"""{"params":{"rootFolder":"${Constants.GAT_FOLDER_IMPORT_PATH}","batchSize":"50","waitMessageTimeoutSeconds":"20","blockAsyncListeners":true}}"""))
      .headers(Headers.base)
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .basicAuth("${adminId}", "${adminPassword}")
  }

}
