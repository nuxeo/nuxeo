/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * @fileoverview API to assist with management of the OAuth popup window.
 */

/**
 * @private
 * @constructor
 */
var gadgets = gadgets || {};

/**
 * @private
 * @constructor
 */
gadgets.oauth = gadgets.oauth || {};

/**
 * @class OAuth popup window manager.
 *
 * <p>
 * Expected usage:
 * </p>
 *
 * <ol>
 * <li>
 * <p>
 * Gadget attempts to fetch OAuth data for the user and discovers that
 * approval is needed.  The gadget creates two new UI elements:
 * </p>
 * <ul>
 *   <li>
 *      a "personalize this gadget" button or link.
 *   </li>
 *   <li>
 *      a "personalization done" button or link, which is initially hidden.
 *   </li>
 * </ul>
 * <p>
 * The "personalization done" button may be unnecessary.  The popup window
 * manager will attempt to detect when the window closes.  However, the 
 * "personalization done" button should still be displayed to handle cases
 * where the popup manager is unable to detect that a window has closed.  This
 * allows the user to signal approval manually.
 * </p>
 * </li>
 *
 * <li>
 * Gadget creates a popup object and associates event handlers with the UI
 * elements:
 *
 * <pre>
 *    // Called when the user opens the popup window.
 *    var onOpen = function() {
 *      $("personalizeDone").style.display = "block"
 *    }
 *    // Called when the user closes the popup window.
 *    var onClose = function() {
 *      $("personalizeDone").style.display = "none"
 *      fetchData();
 *    }
 *    var popup = new gadgets.oauth.Popup(
 *        response.oauthApprovalUrl,
 *        "height=300,width=200",
 *        onOpen,
 *        onClose
 *    );
 *
 *    personalizeButton.onclick = popup.createOpenerOnClick();
 *    personalizeDoneButton.onclick = popup.createApprovedOnClick();
 * </pre>
 * </li>
 *
 * <li>
 * <p>
 * When the user clicks the personalization button/link, a window is opened
 * to the approval URL.  The onOpen function is called to notify the gadget
 * that the window was opened.
 * </p>
 * </li>
 *
 * <li>
 * <p>
 * When the window is closed, the popup manager calls the onClose function
 * and the gadget attempts to fetch the user's data.
 * </p>
 * </li>
 * </ol>
 *
 * @constructor
 *
 * @description used to create a new OAuth popup window manager.
 *
 * @param {String} destination Target URL for the popup window.
 * @param {String} windowOptions Options for window.open, used to specify
 *     look and feel of the window.
 * @param {function} openCallback Function to call when the window is opened.
 * @param {function} closeCallback Function to call when the window is closed.
 */
gadgets.oauth.Popup = function(destination, windowOptions, openCallback,
    closeCallback) {
  this.destination_ = destination;
  this.windowOptions_ = windowOptions;
  this.openCallback_ = openCallback;
  this.closeCallback_ = closeCallback;
  this.win_ = null;
};

/**
 * @return an onclick handler for the "open the approval window" link
 * @type function
 */
gadgets.oauth.Popup.prototype.createOpenerOnClick = function() {
  var self = this;
  return function() {
    self.onClick_();
  };
};

/**
 * Called when the user clicks to open the popup window.
 * 
 * @returns false to prevent the default action for the click.
 * @private
 */
gadgets.oauth.Popup.prototype.onClick_ = function() {
  // If a popup blocker blocks the window, we do nothing.  The user will
  // need to approve the popup, then click again to open the window.
  // Note that because we don't call window.open until the user has clicked
  // something the popup blockers *should* let us through.
  this.win_ = window.open(this.destination_, "_blank", this.windowOptions_);
  if (this.win_) {
    // Poll every 100ms to check if the window has been closed
    var self = this;
    var closure = function() {
      self.checkClosed_(); 
    };
    this.timer_ = window.setInterval(closure, 100);
    this.openCallback_();
  }
  return false;
};

/**
 * Called at intervals to check whether the window has closed.
 * @private
 */
gadgets.oauth.Popup.prototype.checkClosed_ = function() {
  if ((!this.win_) || this.win_.closed) {
    this.win_ = null;
    this.handleApproval_();
  }
};

/**
 * Called when we recieve an indication the user has approved access, either
 * because they closed the popup window or clicked an "I've approved" button.
 * @private
 */
gadgets.oauth.Popup.prototype.handleApproval_ = function() {
  if (this.timer_) {
    window.clearInterval(this.timer_);
    this.timer_ = null;
  }
  if (this.win_) {
    this.win_.close();
    this.win_ = null;
  }
  this.closeCallback_();
  return false;
};

/**
 * @return an onclick handler for the "I've approved" link.  This may not
 * ever be called.  If we successfully detect that the window was closed,
 * this link is unnecessary.
 * @type function
 */
gadgets.oauth.Popup.prototype.createApprovedOnClick = function() {
  var self = this;
  return function() {
    self.handleApproval_();
  };
};
