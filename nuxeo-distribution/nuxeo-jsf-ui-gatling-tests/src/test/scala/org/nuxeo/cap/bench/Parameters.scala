package org.nuxeo.cap.bench

import scala.concurrent.duration.{Duration, FiniteDuration}

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
