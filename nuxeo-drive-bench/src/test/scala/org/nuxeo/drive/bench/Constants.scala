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
 *     Antoine Taillefer
 */
package org.nuxeo.drive.bench

object Constants {
  val API_PATH = "/api/v1/path"
  val AUTOMATION_PATH = "/site/automation"
  val ROOT_WORKSPACE_PATH = "/default-domain/workspaces"

  val GAT_WS_NAME = "Bench_Drive"
  val GAT_WS_PATH = ROOT_WORKSPACE_PATH + "/Bench_Drive"
  val GAT_FOLDER_NAME = "Common"
  val GAT_USER_FOLDER_NAME = "Folder_${user}"
  val GAT_GROUP_NAME = "gatling"

}
