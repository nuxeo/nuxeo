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

object Headers {

  val base = Map(
    "User-Agent" -> "Gatling"
  )

  val nxProperties = base.++(
    Map(
      "X-Nxdocumentproperties" -> "*",
      "X-Nxproperties" -> "*"
    ))

  val withEnricher = nxProperties.++(
    Map(
      "X-NXenrichers.document" -> "files",
      "depth" -> "max"
    ))

  val drive = Map(
    "X-Application-Name" -> "Nuxeo Drive",
    "X-Client-Version" -> "2.0.0625"
  )

}
