/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

/**
 * @fileoverview Open Gadget Container
 *
 * Updated by Nuxeo:
 *   - added method gadgets.StaticLayoutManager.prototype.pushGadgetChromeIds
 * to be able to push new gadgets chrome ids to the existing ones.
 *
 */

var gadgets = gadgets || {};

gadgets.errors = {};
gadgets.errors.SUBCLASS_RESPONSIBILITY = 'subclass responsibility';
gadgets.errors.TO_BE_DONE = 'to be done';

/**
 * Calls an array of asynchronous functions and calls the continuation
 * function when all are done.
 * @param {Array} functions Array of asynchronous functions, each taking
 *     one argument that is the continuation function that handles the result
 *     That is, each function is something like the following:
 *     function(continuation) {
 *       // compute result asynchronously
 *       continuation(result);
 *     }
 * @param {Function} continuation Function to call when all results are in.  It
 *     is pass an array of all results of all functions
 * @param {Object} opt_this Optional object used as "this" when calling each
 *     function
 */
gadgets.callAsyncAndJoin = function(functions, continuation, opt_this) {
  var pending = functions.length;
  var results = [];
  for (var i = 0; i < functions.length; i++) {
    // we need a wrapper here because i changes and we need one index
    // variable per closure
    var wrapper = function(index) {
      functions[index].call(opt_this, function(result) {
        results[index] = result;
        if (--pending === 0) {
          continuation(results);
        }
      });
    };
    wrapper(i);
  }
};


// ----------
// Extensible

gadgets.Extensible = function() {
};

/**
 * Sets the dependencies.
 * @param {Object} dependencies Object whose properties are set on this
 *     container as dependencies
 */
gadgets.Extensible.prototype.setDependencies = function(dependencies) {
  for (var p in dependencies) {
    this[p] = dependencies[p];
  }
};

/**
 * Returns a dependency given its name.
 * @param {String} name Name of dependency
 * @return {Object} Dependency with that name or undefined if not found
 */
gadgets.Extensible.prototype.getDependencies = function(name) {
  return this[name];
};



// -------------
// UserPrefStore

/**
 * User preference store interface.
 * @constructor
 */
gadgets.UserPrefStore = function() {
};

/**
 * Gets all user preferences of a gadget.
 * @param {Object} gadget Gadget object
 * @return {Object} All user preference of given gadget
 */
gadgets.UserPrefStore.prototype.getPrefs = function(gadget) {
  throw Error(gadgets.errors.SUBCLASS_RESPONSIBILITY);
};

/**
 * Saves user preferences of a gadget in the store.
 * @param {Object} gadget Gadget object
 * @param {Object} prefs User preferences
 */
gadgets.UserPrefStore.prototype.savePrefs = function(gadget) {
  throw Error(gadgets.errors.SUBCLASS_RESPONSIBILITY);
};


// -------------
// DefaultUserPrefStore

/**
 * User preference store implementation.
 * TODO: Turn this into a real implementation that is production safe
 * @constructor
 */
gadgets.DefaultUserPrefStore = function() {
  gadgets.UserPrefStore.call(this);
};
gadgets.DefaultUserPrefStore.inherits(gadgets.UserPrefStore);

gadgets.DefaultUserPrefStore.prototype.getPrefs = function(gadget) { };

gadgets.DefaultUserPrefStore.prototype.savePrefs = function(gadget) { };


// -------------
// GadgetService

/**
 * Interface of service provided to gadgets for resizing gadgets,
 * setting title, etc.
 * @constructor
 */
gadgets.GadgetService = function() {
};

gadgets.GadgetService.prototype.setHeight = function(elementId, height) {
  throw Error(gadgets.errors.SUBCLASS_RESPONSIBILITY);
};

gadgets.GadgetService.prototype.setTitle = function(gadget, title) {
  throw Error(gadgets.errors.SUBCLASS_RESPONSIBILITY);
};

gadgets.GadgetService.prototype.setUserPref = function(id) {
  throw Error(gadgets.errors.SUBCLASS_RESPONSIBILITY);
};


// ----------------
// IfrGadgetService

/**
 * Base implementation of GadgetService.
 * @constructor
 */
gadgets.IfrGadgetService = function() {
  gadgets.GadgetService.call(this);
  gadgets.rpc.register('resize_iframe', this.setHeight);
  gadgets.rpc.register('set_pref', this.setUserPref);
  gadgets.rpc.register('set_title', this.setTitle);
  gadgets.rpc.register('requestNavigateTo', this.requestNavigateTo);
  gadgets.rpc.register('requestSendMessage', this.requestSendMessage);
};

gadgets.IfrGadgetService.inherits(gadgets.GadgetService);

gadgets.IfrGadgetService.prototype.setHeight = function(height) {
  if (height > gadgets.container.maxheight_) {
    height = gadgets.container.maxheight_;
  }

  var element = document.getElementById(this.f);
  if (element) {
    element.style.height = height + 'px';
  }
};

gadgets.IfrGadgetService.prototype.setTitle = function(title) {
  var element = document.getElementById(this.f + '_title');
  if (element) {
    element.innerHTML = title.replace(/&/g, '&amp;').replace(/</g, '&lt;');
  }
};

/**
 * Sets one or more user preferences
 * @param {String} editToken
 * @param {String} name Name of user preference
 * @param {String} value Value of user preference
 * More names and values may follow
 */
gadgets.IfrGadgetService.prototype.setUserPref = function(editToken, name,
    value) {
  var id = gadgets.container.gadgetService.getGadgetIdFromModuleId(this.f);
  var gadget = gadgets.container.getGadget(id);
  for (var i = 1, j = arguments.length; i < j; i += 2) {
    gadget.userPrefs[arguments[i]].value = arguments[i + 1];
  }
  gadget.saveUserPrefs();
};

/**
 * Requests the container to send a specific message to the specified users.
 * @param {Array.<String>, String} recipients An ID, array of IDs, or a group reference;
 * the supported keys are VIEWER, OWNER, VIEWER_FRIENDS, OWNER_FRIENDS, or a
 * single ID within one of those groups
 * @param {opensocial.Message} message The message to send to the specified users
 * @param {Function} opt_callback The function to call once the request has been
 * processed; either this callback will be called or the gadget will be reloaded
 * from scratch
 * @param {opensocial.NavigationParameters} opt_params The optional parameters
 * indicating where to send a user when a request is made, or when a request
 * is accepted; options are of type  NavigationParameters.DestinationType
 */
gadgets.IfrGadgetService.prototype.requestSendMessage = function(recipients,
    message, opt_callback, opt_params) {
    if (opt_callback) {
      window.setTimeout(function() {
        opt_callback(new opensocial.ResponseItem(
            null, null, opensocial.ResponseItem.Error.NOT_IMPLEMENTED, null));
      }, 0);
    }
};

/**
 * Navigates the page to a new url based on a gadgets requested view and
 * parameters.
 */
gadgets.IfrGadgetService.prototype.requestNavigateTo = function(view,
    opt_params) {
  var id = gadgets.container.gadgetService.getGadgetIdFromModuleId(this.f);
  var url = gadgets.container.gadgetService.getUrlForView(view);

  if (opt_params) {
    var paramStr = gadgets.json.stringify(opt_params);
    if (paramStr.length > 0) {
      url += '&appParams=' + encodeURIComponent(paramStr);
    }
  }

  if (url && document.location.href.indexOf(url) == -1) {
    document.location.href = url;
  }
};

/**
 * This is a silly implementation that will need to be overriden by almost all
 * real containers.
 * TODO: Find a better default for this function
 *
 * @param view The view name to get the url for
 */
gadgets.IfrGadgetService.prototype.getUrlForView = function(
    view) {
  if (view === 'canvas') {
    return '/canvas';
  } else if (view === 'profile') {
    return '/profile';
  } else {
    return null;
  }
};

gadgets.IfrGadgetService.prototype.getGadgetIdFromModuleId = function(
    moduleId) {
  // Quick hack to extract the gadget id from module id
  return parseInt(moduleId.match(/_([0-9]+)$/)[1], 10);
};


// -------------
// LayoutManager

/**
 * Layout manager interface.
 * @constructor
 */
gadgets.LayoutManager = function() {
};

/**
 * Gets the HTML element that is the chrome of a gadget into which the content
 * of the gadget can be rendered.
 * @param {Object} gadget Gadget instance
 * @return {Object} HTML element that is the chrome for the given gadget
 */
gadgets.LayoutManager.prototype.getGadgetChrome = function(gadget) {
  throw Error(gadgets.errors.SUBCLASS_RESPONSIBILITY);
};

// -------------------
// StaticLayoutManager

/**
 * Static layout manager where gadget ids have a 1:1 mapping to chrome ids.
 * @constructor
 */
gadgets.StaticLayoutManager = function() {
  gadgets.LayoutManager.call(this);
};

gadgets.StaticLayoutManager.inherits(gadgets.LayoutManager);

/**
 * Sets chrome ids, whose indexes are gadget instance ids (starting from 0).
 * @param {Array} gadgetChromeIds Gadget id to chrome id map
 */
gadgets.StaticLayoutManager.prototype.setGadgetChromeIds =
    function(gadgetChromeIds) {
  this.gadgetChromeIds_ = gadgetChromeIds;
};

gadgets.StaticLayoutManager.prototype.pushGadgetChromeIds =
    function(gadgetChromeIds) {
      if (typeof this.gadgetChromeIds_ === 'undefined') {
        this.gadgetChromeIds_ = [];
      }
  this.gadgetChromeIds_ = this.gadgetChromeIds_.concat(gadgetChromeIds);
      console.log(this.gadgetChromeIds_);
};

gadgets.StaticLayoutManager.prototype.getGadgetChrome = function(gadget) {
  var chromeId = this.gadgetChromeIds_[gadget.id];
  return chromeId ? document.getElementById(chromeId) : null;
};


// ----------------------
// FloatLeftLayoutManager

/**
 * FloatLeft layout manager where gadget ids have a 1:1 mapping to chrome ids.
 * @constructor
 * @param {String} layoutRootId Id of the element that is the parent of all
 *     gadgets.
 */
gadgets.FloatLeftLayoutManager = function(layoutRootId) {
  gadgets.LayoutManager.call(this);
  this.layoutRootId_ = layoutRootId;
};

gadgets.FloatLeftLayoutManager.inherits(gadgets.LayoutManager);

gadgets.FloatLeftLayoutManager.prototype.getGadgetChrome =
    function(gadget) {
  var layoutRoot = document.getElementById(this.layoutRootId_);
  if (layoutRoot) {
    var chrome = document.createElement('div');
    chrome.className = 'gadgets-gadget-chrome';
    chrome.style.cssFloat = 'left';
    layoutRoot.appendChild(chrome);
    return chrome;
  } else {
    return null;
  }
};


// ------
// Gadget

/**
 * Creates a new instance of gadget.  Optional parameters are set as instance
 * variables.
 * @constructor
 * @param {Object} params Parameters to set on gadget.  Common parameters:
 *    "specUrl": URL to gadget specification
 *    "private": Whether gadget spec is accessible only privately, which means
 *        browser can load it but not gadget server
 *    "spec": Gadget Specification in XML
 *    "userPrefs": a javascript object containing attribute value pairs of user
 *        preferences for this gadget with the value being a preference object
 *    "viewParams": a javascript object containing attribute value pairs
 *        for this gadgets
 *    "secureToken": an encoded token that is passed on the URL hash
 *    "hashData": Query-string like data that will be added to the
 *        hash portion of the URL.
 *    "specVersion": a hash value used to add a v= param to allow for better caching
 *    "title": the default title to use for the title bar.
 *    "height": height of the gadget
 *    "width": width of the gadget
 *    "debug": send debug=1 to the gadget server, gets us uncompressed
 *        javascript
 */
gadgets.Gadget = function(params) {
  this.userPrefs = {};

  if (params) {
    for (var name in params)  if (params.hasOwnProperty(name)) {
      this[name] = params[name];
    }
  }
  if (!this.secureToken) {
    // Assume that the default security token implementation is
    // in use on the server.
    this.secureToken = 'john.doe:john.doe:appid:cont:url:0:default';
  }
};

gadgets.Gadget.prototype.getUserPrefs = function() {
  return this.userPrefs;
};

gadgets.Gadget.prototype.saveUserPrefs = function() {
  gadgets.container.userPrefStore.savePrefs(this);
};

gadgets.Gadget.prototype.getUserPrefValue = function(name) {
  var pref = this.userPrefs[name];
  return typeof(pref.value) != 'undefined' && pref.value != null ?
      pref.value : pref['default'];
};

gadgets.Gadget.prototype.render = function(chrome) {
  if (chrome) {
    var gadget = this;
    this.getContent(function(content) {
      chrome.innerHTML = content;
      window.frames[gadget.getIframeId()].location = gadget.getIframeUrl();
    });
  }
};

gadgets.Gadget.prototype.getContent = function(continuation) {
  gadgets.callAsyncAndJoin([
      this.getTitleBarContent, this.getUserPrefsDialogContent,
      this.getMainContent], function(results) {
        continuation(results.join(''));
      }, this);
};

/**
 * Gets title bar content asynchronously or synchronously.
 * @param {Function} continuation Function that handles title bar content as
 *     the one and only argument
 */
gadgets.Gadget.prototype.getTitleBarContent = function(continuation) {
  throw Error(gadgets.errors.SUBCLASS_RESPONSIBILITY);
};

/**
 * Gets user preferences dialog content asynchronously or synchronously.
 * @param {Function} continuation Function that handles user preferences
 *     content as the one and only argument
 */
gadgets.Gadget.prototype.getUserPrefsDialogContent = function(continuation) {
  throw Error(gadgets.errors.SUBCLASS_RESPONSIBILITY);
};

/**
 * Gets gadget content asynchronously or synchronously.
 * @param {Function} continuation Function that handles gadget content as
 *     the one and only argument
 */
gadgets.Gadget.prototype.getMainContent = function(continuation) {
  throw Error(gadgets.errors.SUBCLASS_RESPONSIBILITY);
};

/*
 * Gets additional parameters to append to the iframe url
 * Override this method if you need any custom params.
 */
gadgets.Gadget.prototype.getAdditionalParams = function() {
  return '';
};


// ---------
// IfrGadget

gadgets.IfrGadget = function(opt_params) {
  gadgets.Gadget.call(this, opt_params);
  this.serverBase_ = '../../'; // default gadget server
};

gadgets.IfrGadget.inherits(gadgets.Gadget);

gadgets.IfrGadget.prototype.GADGET_IFRAME_PREFIX_ = 'remote_iframe_';

gadgets.IfrGadget.prototype.CONTAINER = 'default';

gadgets.IfrGadget.prototype.cssClassGadget = 'gadgets-gadget';
gadgets.IfrGadget.prototype.cssClassTitleBar = 'gadgets-gadget-title-bar';
gadgets.IfrGadget.prototype.cssClassTitle = 'gadgets-gadget-title';
gadgets.IfrGadget.prototype.cssClassTitleButtonBar =
    'gadgets-gadget-title-button-bar';
gadgets.IfrGadget.prototype.cssClassGadgetUserPrefsDialog =
    'gadgets-gadget-user-prefs-dialog';
gadgets.IfrGadget.prototype.cssClassGadgetUserPrefsDialogActionBar =
    'gadgets-gadget-user-prefs-dialog-action-bar';
gadgets.IfrGadget.prototype.cssClassTitleButton = 'gadgets-gadget-title-button';
gadgets.IfrGadget.prototype.cssClassGadgetContent = 'gadgets-gadget-content';
gadgets.IfrGadget.prototype.rpcToken = (0x7FFFFFFF * Math.random()) | 0;
gadgets.IfrGadget.prototype.rpcRelay = 'files/container/rpc_relay.html';

gadgets.IfrGadget.prototype.getTitleBarContent = function(continuation) {
  var settingsButton = this.hasViewablePrefs_() ?
      '<a href="#" onclick="gadgets.container.getGadget(' + this.id +
          ').handleOpenUserPrefsDialog();return false;" class="' + this.cssClassTitleButton +
          '">settings</a> '
      : '';
  continuation('<div id="' + this.cssClassTitleBar + '-' + this.id +
      '" class="' + this.cssClassTitleBar + '"><span id="' +
      this.getIframeId() + '_title" class="' +
      this.cssClassTitle + '">' + (this.title ? this.title : 'Title') + '</span> | <span class="' +
      this.cssClassTitleButtonBar + '">' + settingsButton +
      '<a href="#" onclick="gadgets.container.getGadget(' + this.id +
      ').handleToggle();return false;" class="' + this.cssClassTitleButton +
      '">toggle</a></span></div>');
};

gadgets.IfrGadget.prototype.getUserPrefsDialogContent = function(continuation) {
  continuation('<div id="' + this.getUserPrefsDialogId() + '" class="' +
      this.cssClassGadgetUserPrefsDialog + '"></div>');
};

gadgets.IfrGadget.prototype.setServerBase = function(url) {
  this.serverBase_ = url;
};

gadgets.IfrGadget.prototype.getServerBase = function() {
  return this.serverBase_;
};

gadgets.IfrGadget.prototype.getMainContent = function(continuation) {
  var iframeId = this.getIframeId();
  gadgets.rpc.setRelayUrl(iframeId, this.serverBase_ + this.rpcRelay);
  gadgets.rpc.setAuthToken(iframeId, this.rpcToken);
  continuation('<div class="' + this.cssClassGadgetContent + '"><iframe id="' +
      iframeId + '" name="' + iframeId + '" class="' + this.cssClassGadget +
      '" src="about:blank' +
      '" frameborder="no" scrolling="no"' +
      (this.height ? ' height="' + this.height + '"' : '') +
      (this.width ? ' width="' + this.width + '"' : '') +
      '></iframe></div>');
};

gadgets.IfrGadget.prototype.getIframeId = function() {
  return this.GADGET_IFRAME_PREFIX_ + this.id;
};

gadgets.IfrGadget.prototype.getUserPrefsDialogId = function() {
  return this.getIframeId() + '_userPrefsDialog';
};

gadgets.IfrGadget.prototype.getIframeUrl = function() {
  return this.serverBase_ + 'ifr?' +
      'container=' + this.CONTAINER +
      '&mid=' +  this.id +
      '&nocache=' + gadgets.container.nocache_ +
      '&country=' + gadgets.container.country_ +
      '&lang=' + gadgets.container.language_ +
      '&view=' + gadgets.container.view_ +
      (this.specVersion ? '&v=' + this.specVersion : '') +
      (gadgets.container.parentUrl_ ? '&parent=' + encodeURIComponent(gadgets.container.parentUrl_) : '') +
      (this.debug ? '&debug=1' : '') +
      this.getAdditionalParams() +
      this.getUserPrefsParams() +
      (this.secureToken ? '&st=' + this.secureToken : '') +
      '&url=' + encodeURIComponent(this.specUrl) +
      '#rpctoken=' + this.rpcToken +
      (this.viewParams ?
          '&view-params=' +  encodeURIComponent(gadgets.json.stringify(this.viewParams)) : '') +
      (this.hashData ? '&' + this.hashData : '');
};

gadgets.IfrGadget.prototype.getUserPrefsParams = function() {
  var params = '';
  for(var name in this.getUserPrefs()) {
    params += '&up_' + encodeURIComponent(name) + '=' +
        encodeURIComponent(this.getUserPrefValue(name));
  }
  return params;
};

gadgets.IfrGadget.prototype.handleToggle = function() {
  var gadgetIframe = document.getElementById(this.getIframeId());
  if (gadgetIframe) {
    var gadgetContent = gadgetIframe.parentNode;
    var display = gadgetContent.style.display;
    gadgetContent.style.display = display ? '' : 'none';
  }
};


gadgets.IfrGadget.prototype.hasViewablePrefs_ = function() {
  for(var name in this.getUserPrefs()) {
    var pref = this.userPrefs[name];
    if (pref.type != 'hidden') {
      return true;
    }
  }
  return false;
};


gadgets.IfrGadget.prototype.handleOpenUserPrefsDialog = function() {
  if (this.userPrefsDialogContentLoaded) {
    this.showUserPrefsDialog();
  } else {
    var gadget = this;
    var igCallbackName = 'ig_callback_' + this.id;
    window[igCallbackName] = function(userPrefsDialogContent) {
      gadget.userPrefsDialogContentLoaded = true;
      gadget.buildUserPrefsDialog(userPrefsDialogContent);
      gadget.showUserPrefsDialog();
    };

    var script = document.createElement('script');
    script.src = 'http://www.gmodules.com/ig/gadgetsettings?mid=' + this.id +
        '&output=js' + this.getUserPrefsParams() +  '&url=' + this.specUrl;
    document.body.appendChild(script);
  }
};

gadgets.IfrGadget.prototype.buildUserPrefsDialog = function(content) {
  var userPrefsDialog = document.getElementById(this.getUserPrefsDialogId());
  userPrefsDialog.innerHTML = content +
      '<div class="' + this.cssClassGadgetUserPrefsDialogActionBar +
      '"><input type="button" value="Save" onclick="gadgets.container.getGadget(' +
      this.id +').handleSaveUserPrefs()"> <input type="button" value="Cancel" onclick="gadgets.container.getGadget(' +
      this.id +').handleCancelUserPrefs()"></div>';
  userPrefsDialog.childNodes[0].style.display = '';
};

gadgets.IfrGadget.prototype.showUserPrefsDialog = function(opt_show) {
  var userPrefsDialog = document.getElementById(this.getUserPrefsDialogId());
  userPrefsDialog.style.display = (opt_show || opt_show === undefined)
      ? '' : 'none';
};

gadgets.IfrGadget.prototype.hideUserPrefsDialog = function() {
  this.showUserPrefsDialog(false);
};

gadgets.IfrGadget.prototype.handleSaveUserPrefs = function() {
  this.hideUserPrefsDialog();

  var numFields = document.getElementById('m_' + this.id +
      '_numfields').value;
  for (var i = 0; i < numFields; i++) {
    var input = document.getElementById('m_' + this.id + '_' + i);
    var userPrefNamePrefix = 'm_' + this.id + '_up_';
    var userPrefName = input.name.substring(userPrefNamePrefix.length);
    var userPrefValue = input.value;
    this.userPrefs[userPrefName].value = userPrefValue;
  }

  this.saveUserPrefs();
  this.refresh();
};

gadgets.IfrGadget.prototype.handleCancelUserPrefs = function() {
  this.hideUserPrefsDialog();
};

gadgets.IfrGadget.prototype.refresh = function() {
  var iframeId = this.getIframeId();
  document.getElementById(iframeId).src = this.getIframeUrl();
};


// ---------
// Container

/**
 * Container interface.
 * @constructor
 */
gadgets.Container = function() {
  this.gadgets_ = {};
  this.parentUrl_ = 'http://' + document.location.host;
  this.country_ = 'ALL';
  this.language_ = 'ALL';
  this.view_ = 'default';
  this.nocache_ = 1;

  // signed max int
  this.maxheight_ = 0x7FFFFFFF;
};

gadgets.Container.inherits(gadgets.Extensible);

/**
 * Known dependencies:
 *     gadgetClass: constructor to create a new gadget instance
 *     userPrefStore: instance of a subclass of gadgets.UserPrefStore
 *     gadgetService: instance of a subclass of gadgets.GadgetService
 *     layoutManager: instance of a subclass of gadgets.LayoutManager
 */

gadgets.Container.prototype.gadgetClass = gadgets.Gadget;

gadgets.Container.prototype.userPrefStore = new gadgets.DefaultUserPrefStore();

gadgets.Container.prototype.gadgetService = new gadgets.GadgetService();

gadgets.Container.prototype.layoutManager =
    new gadgets.StaticLayoutManager();

gadgets.Container.prototype.setParentUrl = function(url) {
  this.parentUrl_ = url;
};

gadgets.Container.prototype.setCountry = function(country) {
  this.country_ = country;
};

gadgets.Container.prototype.setNoCache = function(nocache) {
  this.nocache_ = nocache;
};

gadgets.Container.prototype.setLanguage = function(language) {
  this.language_ = language;
};

gadgets.Container.prototype.setView = function(view) {
  this.view_ = view;
};

gadgets.Container.prototype.setMaxHeight = function(maxheight) {
  this.maxheight_ = maxheight;
};

gadgets.Container.prototype.getGadgetKey_ = function(instanceId) {
  return 'gadget_' + instanceId;
};

gadgets.Container.prototype.getGadget = function(instanceId) {
  return this.gadgets_[this.getGadgetKey_(instanceId)];
};

gadgets.Container.prototype.createGadget = function(opt_params) {
  return new this.gadgetClass(opt_params);
};

gadgets.Container.prototype.addGadget = function(gadget) {
  gadget.id = this.getNextGadgetInstanceId();
  this.gadgets_[this.getGadgetKey_(gadget.id)] = gadget;
};

gadgets.Container.prototype.addGadgets = function(gadgets) {
  for (var i = 0; i < gadgets.length; i++) {
    this.addGadget(gadgets[i]);
  }
};

/**
 * Renders all gadgets in the container.
 */
gadgets.Container.prototype.renderGadgets = function() {
  for (var key in this.gadgets_) {
    this.renderGadget(this.gadgets_[key]);
  }
};

/**
 * Renders a gadget.  Gadgets are rendered inside their chrome element.
 * @param {Object} gadget Gadget object
 */
gadgets.Container.prototype.renderGadget = function(gadget) {
  throw Error(gadgets.errors.SUBCLASS_RESPONSIBILITY);
};

gadgets.Container.prototype.nextGadgetInstanceId_ = 0;

gadgets.Container.prototype.getNextGadgetInstanceId = function() {
  return this.nextGadgetInstanceId_++;
};

/**
 * Refresh all the gadgets in the container.
 */
gadgets.Container.prototype.refreshGadgets = function() {
  for (var key in this.gadgets_) {
    this.gadgets_[key].refresh();
  }
};


// ------------
// IfrContainer

/**
 * Container that renders gadget using ifr.
 * @constructor
 */
gadgets.IfrContainer = function() {
  gadgets.Container.call(this);
};

gadgets.IfrContainer.inherits(gadgets.Container);

gadgets.IfrContainer.prototype.gadgetClass = gadgets.IfrGadget;

gadgets.IfrContainer.prototype.gadgetService = new gadgets.IfrGadgetService();

gadgets.IfrContainer.prototype.setParentUrl = function(url) {
  if (!url.match(/^http[s]?:\/\//)) {
    url = document.location.href.match(/^[^?#]+\//)[0] + url;
  }

  this.parentUrl_ = url;
};

/**
 * Renders a gadget using ifr.
 * @param {Object} gadget Gadget object
 */
gadgets.IfrContainer.prototype.renderGadget = function(gadget) {
  var chrome = this.layoutManager.getGadgetChrome(gadget);
  gadget.render(chrome);
};

/**
 * Default container.
 */
gadgets.container = new gadgets.IfrContainer();
