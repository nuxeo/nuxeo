/*
 * (C) Copyright 2016-2019 Nuxeo (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.drive.bench

import scala.concurrent.duration.{Duration, FiniteDuration}

object Parameters {

  def getBaseUrl(default: String = "http://localhost:8080/nuxeo"): String = {
    System.getProperty("url", default)
  }

  def getSimulationDuration(default: Integer = 60): Duration = {
    val duration: Long = 0L + Integer.getInteger("duration", default)
    Duration(duration, "second")
  }

  def getConcurrentUsers(default: Integer = 8, prefix: String = ""): Integer = {
    Integer.getInteger(prefix + "users", default)
  }

  def getConcurrentWriters(default: Integer = 10, prefix: String = ""): Integer = {
    Integer.getInteger(prefix + "writers", default)
  }

  def getRampDuration(default: Integer = 2, prefix: String = ""): FiniteDuration = {
    val ramp: Long = 0L + Integer.getInteger(prefix + "ramp", default)
    FiniteDuration(ramp, "second")
  }

  def getPollInterval(default: Integer = 30): Integer = {
    Integer.getInteger("pollInterval", default)
  }

  def getFeederInterval(default: Integer = 10): Integer = {
    Integer.getInteger("feederInterval", default)
  }

  def getNbThreads(default: Integer = 12): Integer = {
    Integer.getInteger("nbThreads", default)
  }

  def getNbNodes(default: Integer = 100000): Integer = {
    Integer.getInteger("nbNodes", default)
  }

  def getDescendantsBatchSize(default: Integer = 100): Integer = {
    Integer.getInteger("batchSize", default)
  }

  def getPause(defaultMs: Integer = 0, prefix: String = ""): Duration = {
    val pauseMs: Long = 0L + Integer.getInteger(prefix + "pauseMs", defaultMs)
    Duration(pauseMs, "millisecond")
  }

}
