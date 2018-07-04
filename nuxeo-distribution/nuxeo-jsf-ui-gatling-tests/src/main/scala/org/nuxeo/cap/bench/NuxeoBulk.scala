/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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

  def bulkUpdateDocument = () => {
    http("Run a bulk operation")
      .post(Constants.AUTOMATION_PATH + "/Bulk.RunAction")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("content-type", "application/json+nxrequest")
      .body(StringBody( """{"params":{"query":"Select * from Document", "action":"setProperties", "parameters":{"dc:description":"testbulk"}},"context":{}}""")
    )
  }

  def waitForAction(commandId: String) = {
    http("Wait for action to be completed")
      .post(Constants.AUTOMATION_PATH + "/Bulk.WaitForAction")
      .basicAuth("${adminId}", "${adminPassword}")
      .headers(Headers.base)
      .header("content-type", "application/json+nxrequest")
      .body(StringBody( """{"params":{"timeoutSecond": "3600", "commandId": "${commandId}"},"context":{}}"""))
  }

}
