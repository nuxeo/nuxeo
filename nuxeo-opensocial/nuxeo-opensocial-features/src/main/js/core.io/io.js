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

/*global ActiveXObject, DOMParser */
/*global shindig */

var gadgets = gadgets || {};

/**
 * @fileoverview Provides remote content retrieval facilities.
 *     Available to every gadget.
 */

/**
 * @static
 * @class Provides remote content retrieval functions.
 * @name gadgets.io
 */

gadgets.io = function() {
  /**
   * Holds configuration-related data such as proxy urls.
   */
  var config = {};

  /**
   * Holds state for OAuth.
   */
  var oauthState;

  /**
   * Internal facility to create an xhr request.
   */
  function makeXhr() {
    var x;
    if (window.ActiveXObject) {
      x = new ActiveXObject("Msxml2.XMLHTTP");
      if (!x) {
        x = new ActiveXObject("Microsoft.XMLHTTP");
      }
      return x;
    }
    else if (window.XMLHttpRequest) {
      return new window.XMLHttpRequest();
    }
  }

  /**
   * Checks the xobj for errors, may call the callback with an error response
   * if the error is fatal.
   *
   * @param {Object} xobj The XHR object to check
   * @param {Function} callback The callback to call if the error is fatal
   * @return true if the xobj is not ready to be processed
   */
  function hadError(xobj, callback) {
    if (xobj.readyState !== 4) {
      return true;
    }
    try {
      if (xobj.status !== 200) {
        var error = ("" + xobj.status);
        if(xobj.responseText) {
          error = error + " " + xobj.responseText;
        }
        callback({
          errors : [error],
          rc : xobj.status,
          text : xobj.responseText
          });
        return true;
      }
    } catch(e) {
      callback({
         errors : [e.number + " Error not specified"],
          rc : e.number,
          text : e.description
      });
      return true;
    }
    return false;
  }

  /**
   * Handles non-proxied XHR callback processing.
   *
   * @param {String} url
   * @param {Function} callback
   * @param {Object} params
   * @param {Object} xobj
   */
  function processNonProxiedResponse(url, callback, params, xobj) {
    if (hadError(xobj, callback)) {
      return;
    }
    var data = {
      body: xobj.responseText
    };
    callback(transformResponseData(params, data));
  }

  var UNPARSEABLE_CRUFT = "throw 1; < don't be evil' >";

  /**
   * Handles XHR callback processing.
   *
   * @param {String} url
   * @param {Function} callback
   * @param {Object} params
   * @param {Object} xobj
   */
  function processResponse(url, callback, params, xobj) {
    if (hadError(xobj, callback)) {
      return;
    }
    var txt = xobj.responseText;
    // remove unparseable cruft used to prevent cross-site script inclusion
    txt = txt.substr(UNPARSEABLE_CRUFT.length);
    // We are using eval directly here because the outer response comes from a
    // trusted source, and json parsing is slow in IE.
    var data = eval("(" + txt + ")");
    data = data[url];
    // Save off any transient OAuth state the server wants back later.
    if (data.oauthState) {
      oauthState = data.oauthState;
    }
    // Update the security token if the server sent us a new one
    if (data.st) {
      shindig.auth.updateSecurityToken(data.st);
    }
    callback(transformResponseData(params, data));
  }

  function transformResponseData(params, data) {
    // Sometimes rc is not present, generally when used
    // by jsonrpccontainer, so assume 200 in its absence.
    var resp = {
     text: data.body,
     rc: data.rc || 200,
     headers: data.headers,
     oauthApprovalUrl: data.oauthApprovalUrl,
     oauthError: data.oauthError,
     oauthErrorText: data.oauthErrorText,
     errors: []
    };

    if (resp.rc < 200 || resp.rc >= 400){
      resp.errors = [resp.rc + " Error"]
    } else if (resp.text) {
      if (resp.rc >= 300 && resp.rc < 400) {
        // Redirect pages will usually contain arbitrary
        // HTML which will fail during parsing, inadvertently
        // causing a 500 response. Thus we treat as text.
        params.CONTENT_TYPE = "TEXT";
      }
      switch (params.CONTENT_TYPE) {
        case "JSON":
        case "FEED":
          resp.data = gadgets.json.parse(resp.text);
          if (!resp.data) {
            resp.errors.push("500 Failed to parse JSON");
            resp.rc = 500;
            resp.data = null;
          }
          break;
        case "DOM":
          var dom;
          if (window.ActiveXObject) {
            dom = new ActiveXObject("Microsoft.XMLDOM");
            dom.async = false;
            dom.validateOnParse = false;
            dom.resolveExternals = false;
            if (!dom.loadXML(resp.text)) {
              resp.errors.push("500 Failed to parse XML");
              resp.rc = 500;
            } else {
              resp.data = dom;
            }
          } else {
            var parser = new DOMParser();
            dom = parser.parseFromString(resp.text, "text/xml");
            if ("parsererror" === dom.documentElement.nodeName) {
              resp.errors.push("500 Failed to parse XML");
              resp.rc = 500;
            } else {
              resp.data = dom;
            }
          }
          break;
        default:
          resp.data = resp.text;
          break;
      }
  }
    return resp;
  }

  /**
   * Sends an XHR post or get request
   *
   * @param realUrl The url to fetch data from that was requested by the gadget
   * @param proxyUrl The url to proxy through
   * @param callback The function to call once the data is fetched
   * @param postData The data to post to the proxyUrl
   * @param params The params to use when processing the response
   * @param processResponseFunction The function that should process the
   *     response from the sever before calling the callback
   */
  function makeXhrRequest(realUrl, proxyUrl, callback, paramData, method,
      params, processResponseFunction, opt_contentType) {
    var xhr = makeXhr();

    if (proxyUrl.indexOf('//') == 0) {
      proxyUrl = document.location.protocol + proxyUrl;
    }

    xhr.open(method, proxyUrl, true);
    if (callback) {
      xhr.onreadystatechange = gadgets.util.makeClosure(
          null, processResponseFunction, realUrl, callback, params, xhr);
    }
    if (paramData !== null) {
      xhr.setRequestHeader('Content-Type', opt_contentType || 'application/x-www-form-urlencoded');
      xhr.send(paramData);
    } else {
      xhr.send(null);
    }
  }



  /**
   * Satisfy a request with data that is prefetched as per the gadget Preload
   * directive. The preloader will only satisfy a request for a specific piece
   * of content once.
   *
   * @param postData The definition of the request to be executed by the proxy
   * @param params The params to use when processing the response
   * @param callback The function to call once the data is fetched
   * @return true if the request can be satisfied by the preloaded content
   *         false otherwise
   */
  function respondWithPreload(postData, params, callback) {
    if (gadgets.io.preloaded_ && postData.httpMethod === "GET") {
      for (var i = 0; i < gadgets.io.preloaded_.length; i++) {
        var preload = gadgets.io.preloaded_[i];
        if (preload && (preload.id === postData.url)) {
          // Only satisfy once
          delete gadgets.io.preloaded_[i];

          if (preload.rc !== 200) {
            callback({rc: preload.rc, errors : [preload.rc + " Error"]});
          } else {
            if (preload.oauthState) {
              oauthState = preload.oauthState;
            }
            var resp = {
              body: preload.body,
              rc: preload.rc,
              headers: preload.headers,
              oauthApprovalUrl: preload.oauthApprovalUrl,
              oauthError: preload.oauthError,
              oauthErrorText: preload.oauthErrorText,
              errors: []
            };
            callback(transformResponseData(params, resp));
          }
          return true;
        }
      }
    }
    return false;
  }

  /**
   * @param {Object} configuration Configuration settings
   * @private
   */
  function init (configuration) {
    config = configuration["core.io"] || {};
  }

  var requiredConfig = {
    proxyUrl: new gadgets.config.RegExValidator(/.*%(raw)?url%.*/),
    jsonProxyUrl: gadgets.config.NonEmptyStringValidator
  };
  gadgets.config.register("core.io", requiredConfig, init);

  return /** @scope gadgets.io */ {
    /**
     * Fetches content from the provided URL and feeds that content into the
     * callback function.
     *
     * Example:
     * <pre>
     * gadgets.io.makeRequest(url, fn,
     *    {contentType: gadgets.io.ContentType.FEED});
     * </pre>
     *
     * @param {String} url The URL where the content is located
     * @param {Function} callback The function to call with the data from the
     *     URL once it is fetched
     * @param {Map.&lt;gadgets.io.RequestParameters, Object&gt;} opt_params
     *     Additional
     *     <a href="gadgets.io.RequestParameters.html">parameters</a>
     *     to pass to the request
     *
     * @member gadgets.io
     */
    makeRequest : function (url, callback, opt_params) {
      // TODO: This method also needs to respect all members of
      // gadgets.io.RequestParameters, and validate them.

      var params = opt_params || {};

      var httpMethod = params.METHOD || "GET";
      var refreshInterval = params.REFRESH_INTERVAL;

      // Check if authorization is requested
      var auth, st;
      if (params.AUTHORIZATION && params.AUTHORIZATION !== "NONE") {
        auth = params.AUTHORIZATION.toLowerCase();
        st = shindig.auth.getSecurityToken();
      } else {
        // Unauthenticated GET requests are cacheable
        if (httpMethod === "GET" && refreshInterval === undefined) {
          refreshInterval = 3600;
        }
      }

      // Include owner information?
      var signOwner = true;
      if (typeof params.OWNER_SIGNED !== "undefined") {
        signOwner = params.OWNER_SIGNED;
      }

      // Include viewer information?
      var signViewer = true;
      if (typeof params.VIEWER_SIGNED !== "undefined") {
        signViewer = params.VIEWER_SIGNED;
      }

      var headers = params.HEADERS || {};
      if (httpMethod === "POST" && !headers["Content-Type"]) {
        headers["Content-Type"] = "application/x-www-form-urlencoded";
      }

      var urlParams = gadgets.util.getUrlParameters();

      var paramData = {
        url: url,
        httpMethod : httpMethod,
        headers: gadgets.io.encodeValues(headers, false),
        postData : params.POST_DATA || "",
        authz : auth || "",
        st : st || "",
        contentType : params.CONTENT_TYPE || "TEXT",
        numEntries : params.NUM_ENTRIES || "3",
        getSummaries : !!params.GET_SUMMARIES,
        signOwner : signOwner,
        signViewer : signViewer,
        gadget : urlParams.url,
        container : urlParams.container || urlParams.synd || "default",
        // should we bypass gadget spec cache (e.g. to read OAuth provider URLs)
        bypassSpecCache : gadgets.util.getUrlParameters().nocache || ""
      };

      // OAuth goodies
      if (auth === "oauth" || auth === "signed") {
        if (gadgets.io.oauthReceivedCallbackUrl_) {
          paramData.OAUTH_RECEIVED_CALLBACK = gadgets.io.oauthReceivedCallbackUrl_;
          gadgets.io.oauthReceivedCallbackUrl_ = null;
        }
        paramData.oauthState = oauthState || "";
        // Just copy the OAuth parameters into the req to the server
        for (opt in params) {
          if (params.hasOwnProperty(opt)) {
            if (opt.indexOf("OAUTH_") === 0) {
              paramData[opt] = params[opt];
            }
          }
        }
      }

      var proxyUrl = config.jsonProxyUrl.replace("%host%", document.location.host);
      // Nuxeo change: added %protocol%
      proxyUrl = proxyUrl.replace("%protocol%", document.location.protocol);


      if (!respondWithPreload(paramData, params, callback, processResponse)) {
        if (httpMethod === "GET" && refreshInterval > 0) {
          // this content should be cached
          // Add paramData to the URL
          var extraparams = "?refresh=" + refreshInterval + '&'
              + gadgets.io.encodeValues(paramData);

          makeXhrRequest(url, proxyUrl + extraparams, callback,
              null, "GET", params, processResponse);

        } else {
          makeXhrRequest(url, proxyUrl, callback,
              gadgets.io.encodeValues(paramData), "POST", params,
              processResponse);
        }
      }
    },

    /**
     * @private
     */
    makeNonProxiedRequest : function (relativeUrl, callback, opt_params, opt_contentType) {
      var params = opt_params || {};
      makeXhrRequest(relativeUrl, relativeUrl, callback, params.POST_DATA,
          params.METHOD, params, processNonProxiedResponse, opt_contentType);
    },

    /**
     * Used to clear out the oauthState, for testing only.
     *
     * @private
     */
    clearOAuthState : function () {
      oauthState = undefined;
    },

    /**
     * Converts an input object into a URL-encoded data string.
     * (key=value&amp;...)
     *
     * @param {Object} fields The post fields you wish to encode
     * @param {Boolean} opt_noEscaping An optional parameter specifying whether
     *     to turn off escaping of the parameters. Defaults to false.
     * @return {String} The processed post data in www-form-urlencoded format.
     *
     * @member gadgets.io
     */
    encodeValues : function (fields, opt_noEscaping) {
      var escape = !opt_noEscaping;

      var buf = [];
      var first = false;
      for (var i in fields) {
        if (fields.hasOwnProperty(i)) {
          if (!first) {
            first = true;
          } else {
            buf.push("&");
          }
          buf.push(escape ? encodeURIComponent(i) : i);
          buf.push("=");
          buf.push(escape ? encodeURIComponent(fields[i]) : fields[i]);
        }
      }
      return buf.join("");
    },

    /**
     * Gets the proxy version of the passed-in URL.
     *
     * @param {String} url The URL to get the proxy URL for
     * @param {Object} opt_params Optional Parameter Object.
     *     The following properties are supported:
     *       .REFRESH_INTERVAL The number of seconds that this
     *           content should be cached.  Defaults to 3600.
     *
     * @return {String} The proxied version of the URL
     *
     * @member gadgets.io
     */
    getProxyUrl : function (url, opt_params) {
      var params = opt_params || {};
      var refresh = params.REFRESH_INTERVAL;
      if (refresh === undefined) {
        refresh = "3600";
      }

      var urlParams = gadgets.util.getUrlParameters();

      var rewriteMimeParam =
          params.rewriteMime ? "&rewriteMime=" + encodeURIComponent(params.rewriteMime) : "";
      return config.proxyUrl.replace("%url%", encodeURIComponent(url)).
          replace("%host%", document.location.host).
          replace("%rawurl%", url).
          replace("%refresh%", encodeURIComponent(refresh)).
          replace("%gadget%", encodeURIComponent(urlParams.url)).
          replace("%container%", encodeURIComponent(urlParams.container || urlParams.synd)).
          replace("%rewriteMime%", rewriteMimeParam);
    }
  };
}();

gadgets.io.RequestParameters = gadgets.util.makeEnum([
  "METHOD",
  "CONTENT_TYPE",
  "POST_DATA",
  "HEADERS",
  "AUTHORIZATION",
  "NUM_ENTRIES",
  "GET_SUMMARIES",
  "REFRESH_INTERVAL",
  "OAUTH_SERVICE_NAME",
  "OAUTH_USE_TOKEN",
  "OAUTH_TOKEN_NAME",
  "OAUTH_REQUEST_TOKEN",
  "OAUTH_REQUEST_TOKEN_SECRET",
  "OAUTH_RECEIVED_CALLBACK"
]);

gadgets.io.MethodType = gadgets.util.makeEnum([
  "GET", "POST", "PUT", "DELETE", "HEAD"
]);

gadgets.io.ContentType = gadgets.util.makeEnum([
  "TEXT", "DOM", "JSON", "FEED"
]);

gadgets.io.AuthorizationType = gadgets.util.makeEnum([
  "NONE", "SIGNED", "OAUTH"
]);
