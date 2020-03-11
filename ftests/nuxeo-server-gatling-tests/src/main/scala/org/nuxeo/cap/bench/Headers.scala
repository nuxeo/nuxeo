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

}
