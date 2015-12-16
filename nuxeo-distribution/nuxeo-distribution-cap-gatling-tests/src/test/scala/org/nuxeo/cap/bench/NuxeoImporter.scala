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

object NuxeoImporter {

  val API_PATH = "/site/randomImporter"

  def massImport(nbThreads: Integer, nbNodes: Integer) = {
    http("Mass import " + nbNodes.toString() + " with " + nbThreads.toString() + " threads")
      .get(API_PATH + "/run")
      .queryParam("targetPath", Constants.GAT_WS_PATH)
      .queryParam("batchSize", "50")
      .queryParam("nbThreads", nbThreads)
      .queryParam("interactive", "true")
      .queryParam("fileSizeKB", "1")
      .queryParam("nbNodes", nbNodes)
      .queryParam("transactionTimeout", "3600")
      .headers(Headers.base)
      .basicAuth("${adminId}", "${adminPassword}")
  }

  def waitForAsyncJobs() = {
    http("Wait for async jobs")
      .get(API_PATH + "/waitForAsyncJobs")
      .queryParam("timeoutInSeconds", "3600")
      .headers(Headers.base)
      .basicAuth("${adminId}", "${adminPassword}")
  }
}
