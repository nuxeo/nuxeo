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
