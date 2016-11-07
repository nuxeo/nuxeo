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
 *     bdelbosc
 */

object Headers {

  val base = Map(
    "User-Agent" -> "Gatling",
    "Accept-Language" -> "en-us"
  )

  val default = base.++(
    Map(
      "X-Nxdocumentproperties" -> "*",
      "X-Nxproperties" -> "*"
    ))

  val withEnricher = default.++(
    Map(
      "X-NXenrichers.document" -> "files",
      "depth" -> "max"))

  val jsfHeader = Map(
    "Accept-Language" -> "en-us",
    "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "User-Agent" -> "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:40.0) Gecko/20100101 Firefox/Gatling/40.0"
  )


}
