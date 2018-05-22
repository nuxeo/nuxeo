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
