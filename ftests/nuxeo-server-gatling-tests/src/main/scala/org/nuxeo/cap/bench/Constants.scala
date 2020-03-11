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

object Constants {
  val API_PATH = "/api/v1/path"
  val API_QUERY = "/api/v1/query"
  val API_SEARCH = "/api/v1/search"
  val AUTOMATION_PATH = "/site/automation"
  val ROOT_WORKSPACE_PATH = "/default-domain/workspaces"
  val NX_PATH = "/nxpath/default"


  val GAT_WS_NAME = "Bench_Gatling"
  val GAT_WS_PATH = ROOT_WORKSPACE_PATH + "/Bench_Gatling"
  val GAT_FOLDER_NAME = "Common"
  val GAT_USER_FOLDER_NAME = "Folder_${user}"
  val GAT_GROUP_NAME = "gatling"
  val GAT_API_PATH = API_PATH + GAT_WS_PATH

  val EMPTY_MARKER = "EMPTY_MARKER"
  val END_OF_FEED = "END_OF_FEED"
}
