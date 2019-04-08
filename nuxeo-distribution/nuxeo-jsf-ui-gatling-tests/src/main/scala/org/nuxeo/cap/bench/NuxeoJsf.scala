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

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
 * Current user and document are provided by feeders
 */
object NuxeoJsf {

  def viewLoginPage() = {
    http("View login page")
      .post("/")
      .headers(Headers.jsfHeader)
      .check(status.is(200))
  }

  /** login with the current user, and redirect to the path */
  def loginAndGoTo(description: String, path: String) = {
    http(description)
      .post("/nxstartup.faces")
      .headers(Headers.jsfHeader)
      .formParam("user_name", "${user}")
      .formParam("user_password", "${password}")
      .formParam("language", "en")
      .formParam("requestedUrl", path)
      .formParam("forceAnonymousLogin", "")
      .formParam("form_submitted_marker", "")
      .formParam("Submit", "Connexon")
      .check(status.is(200)).check(currentLocationRegex(".*loginFailed.*").notExists)
  }

  /** login with the current user, redirected to dashboard. */
  def login() = {
    loginAndGoTo("login", "")
  }

  /** login with the current user, redirected to Gatling WS */
  def loginAndGoToGatlingWorkspace() = {
    loginAndGoTo("Login redirect to workspace", "nxpath/default" + Constants.GAT_WS_PATH +
      "@view_documents?tabIds=%3A").check(currentLocationRegex(".*" + Constants.GAT_WS_PATH + ".*").exists)
  }

  def logout() = {
    http("Logout")
      .post("/logout")
      .check(status.is(401))
  }

  def viewDocumentTab(description: String, path: String, tab: String) = {
    http(description)
      .get(Constants.NX_PATH + path + "@view_documents??tabIds=" + tab + "&conversationId=0NXMAIN")
      .headers(Headers.jsfHeader)
      .check(status.is(200))
  }

  def viewCurrentDocument() = {
    viewDocumentTab("View document", Constants.GAT_WS_PATH + "/${url}", "%3A")
  }

  def viewCurrentDocumentEditTab() = {
    viewDocumentTab("View document edit tab", Constants.GAT_WS_PATH + "/${url}", "%3ATAB_EDIT")
  }

  def viewCurrentDocumentPermissionTab() = {
    viewDocumentTab("View document permission tab", Constants.GAT_WS_PATH + "/${url}", "%3ATAB_PERMISSIONS")
  }

  def viewCurrentDocumentHistoryTab() = {
    viewDocumentTab("View document history tab", Constants.GAT_WS_PATH + "/${url}", "%3ATAB_CONTENT_HISTORY")
  }

  def viewCurrentDocumentManageTab() = {
    viewDocumentTab("View document manage tab", Constants.GAT_WS_PATH + "/${url}", "%3ATAB_MANAGE")
  }

  def viewCurrentDocumentManageSubscriptionTab() = {
    viewDocumentTab("View document manage subscription tab", Constants.GAT_WS_PATH + "/${url}",
      "%3ATAB_MANAGE%3ATAB_MANAGE_SUBSCRIPTIONS")
  }

  def viewCurrentDocumentTrashTab() = {
    viewDocumentTab("View document trash tab", Constants.GAT_WS_PATH + "/${url}", "%3ATAB_MANAGE%3ATAB_TRASH_CONTENT")
  }

  def viewCurrentDocumentFilesTab() = {
    viewDocumentTab("View document files tab", Constants.GAT_WS_PATH + "/${url}", "%3ATAB_FILES_EDIT")
  }

  def viewCurrentDocumentRelationTab() = {
    viewDocumentTab("View document relation tab", Constants.GAT_WS_PATH + "/${url}", "%3ATAB_RELATIONS")
  }

  def viewCurrentDocumentPublishTab() = {
    viewDocumentTab("View document publish tab", Constants.GAT_WS_PATH + "/${url}", "%3ATAB_PUBLISH")
  }

  def viewCurrentDocumentCommentsTab() = {
    viewDocumentTab("View document publish tab", Constants.GAT_WS_PATH + "/${url}", "%3Aview_comments")
  }

  def viewParentFolderOfCurrentDocument() = {
    viewDocumentTab("View parent folder", Constants.GAT_WS_PATH + "/${parentPath}", "%3A")
  }

}
