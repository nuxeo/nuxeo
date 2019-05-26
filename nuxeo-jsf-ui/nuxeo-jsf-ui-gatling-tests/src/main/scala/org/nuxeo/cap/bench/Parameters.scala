/*
 * (C) Copyright 2015-2019 Nuxeo (http://nuxeo.com/) and contributors.
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

import scala.concurrent.duration.{Duration, FiniteDuration}

object Parameters {

  def getBaseUrl(default: String = "http://localhost:8080/nuxeo"): String = {
    System.getProperty("url", default)
  }

  def getConcurrentUsers(default: Integer = 8, prefix: String = ""): Integer = {
    Integer.getInteger(prefix + "users", default)
  }

  def getPause(defaultMs: Integer = 0, prefix: String = ""): Duration = {
    val pauseMs: Long = 0L + Integer.getInteger(prefix + "pause_ms", defaultMs)
    Duration(pauseMs, "millisecond")
  }

  def getSimulationDuration(default: Integer = 120): Duration = {
    val duration: Long = 0L + Integer.getInteger("duration", default)
    Duration(duration, "second")
  }

  def getRampDuration(default: Integer = 2, prefix: String = ""): FiniteDuration = {
    val ramp: Long = 0L + Integer.getInteger(prefix + "ramp", default)
    FiniteDuration(ramp, "second")
  }

  def getNbNodes(default: Integer = 100000): Integer = {
    Integer.getInteger("nbNodes", default)
  }


}
