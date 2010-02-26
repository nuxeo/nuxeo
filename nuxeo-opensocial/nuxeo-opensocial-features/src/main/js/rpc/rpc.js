/*
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
 * @fileoverview Remote procedure call library for gadget-to-container,
 * container-to-gadget, and gadget-to-gadget (thru container) communication.
 */
var gadgets = gadgets || {};

/**
 * gadgets.rpc Transports
 *
 * All transports are stored in object gadgets.rpctx, and are provided
 * to the core gadgets.rpc library by various build rules.
 * 
 * Transports used by core gadgets.rpc code to actually pass messages.
 * each transport implements the same interface exposing hooks that
 * the core library calls at strategic points to set up and use
 * the transport.
 *
 * The methods each transport must implement are:
 * + getCode(): returns a string identifying the transport. For debugging.
 * + isParentVerifiable(): indicates (via boolean) whether the method
 *     has the property that its relay URL verifies for certain the
 *     receiver's protocol://host:port.
 * + init(processFn, readyFn): Performs any global initialization needed. Called
 *     before any other gadgets.rpc methods are invoked. processFn is
 *     the function in gadgets.rpc used to process an rpc packet. readyFn is
 *     a function that must be called when the transport is ready to send
 *     and receive messages bidirectionally. Returns
 *     true if successful, false otherwise.
 * + setup(receiverId, token): Performs per-receiver initialization, if any.
 *     receiverId will be '..' for gadget-to-container. Returns true if
 *     successful, false otherwise.
 * + call(targetId, from, rpc): Invoked to send an actual
 *     message to the given targetId, with the given serviceName, from
 *     the sender identified by 'from'. Payload is an rpc packet. Returns
 *     true if successful, false otherwise.
 */

/**
 * @static
 * @class Provides operations for making rpc calls.
 * @name gadgets.rpc
 */
gadgets.rpc = function() {
  // General constants.
  var CALLBACK_NAME = '__cb';
  var DEFAULT_NAME = '';

  // Special service name for acknowledgements.
  var ACK = '__ack';

  // Timeout and number of attempts made to setup a transport receiver.
  var SETUP_FRAME_TIMEOUT = 500;
  var SETUP_FRAME_MAX_TRIES = 10;

  var services = {};
  var relayUrl = {};
  var useLegacyProtocol = {};
  var authToken = {};
  var callId = 0;
  var callbacks = {};
  var setup = {};
  var sameDomain = {};
  var params = {};
  var receiverTx = {};
  var earlyRpcQueue = {};

  // isGadget =~ isChild for the purposes of rpc (used only in setup).
  var isChild = (window.top !== window.self);

  // Set the current rpc ID from window.name immediately, to prevent
  // shadowing of window.name by a "var name" declaration, or similar.
  var rpcId = window.name;

  // Fallback transport is simply a dummy impl that emits no errors
  // and logs info on calls it receives, to avoid undesired side-effects
  // from falling back to IFPC or some other transport.
  var fallbackTransport = (function() {
    function logFn(name) {
      return function() {
        gadgets.log("gadgets.rpc." + name + "(" +
                    gadgets.json.stringify(Array.prototype.slice.call(arguments)) +
                    "): call ignored. [caller: " + document.location +
                    ", isChild: " + isChild + "]");
      }
    }
    return {
      getCode: function() {
        return "noop";
      },
      isParentVerifiable: function() {
        return true;  // Not really, but prevents transport assignment to IFPC.
      },
      init: logFn("init"),
      setup: logFn("setup"),
      call: logFn("call")
    }
  })();

  // Load the authentication token for speaking to the container
  // from the gadget's parameters, or default to '0' if not found.
  if (gadgets.util) {
    params = gadgets.util.getUrlParameters();
  }

  // Indicates whether to support early-message queueing, which is designed
  // to ensure that all messages sent by gadgets.rpc.call, irrespective
  // when they were made (before/after setupReceiver, before/after transport
  // setup complete), are sent. Hiding behind a query param to allow opt-in
  // for a time while this technique is proven.
  var useEarlyQueueing = (params['rpc_earlyq'] === "1");

  /*
   * Return a transport representing the best available cross-domain
   * message-passing mechanism available to the browser.
   *
   * Transports are selected on a cascading basis determined by browser
   * capability and other checks. The order of preference is:
   * 1. wpm: Uses window.postMessage standard.
   * 2. dpm: Uses document.postMessage, similar to wpm but pre-standard.
   * 3. nix: Uses IE-specific browser hacks.
   * 4. rmr: Signals message passing using relay file's onresize handler.
   * 5. fe: Uses FF2-specific window.frameElement hack.
   * 6. ifpc: Sends messages via active load of a relay file.
   *
   * See each transport's commentary/documentation for details.
   */
  function getTransport() {
    return typeof window.postMessage === 'function' ? gadgets.rpctx.wpm :
           typeof window.postMessage === 'object' ? gadgets.rpctx.wpm :
           window.ActiveXObject ? gadgets.rpctx.nix :
           navigator.userAgent.indexOf('WebKit') > 0 ? gadgets.rpctx.rmr :
           navigator.product === 'Gecko' ? gadgets.rpctx.frameElement :
           gadgets.rpctx.ifpc;
  }

  /**
   * Function passed to, and called by, a transport indicating it's ready to
   * send and receive messages.
   */
  function transportReady(receiverId, readySuccess) {
    var tx = transport;
    if (!readySuccess) {
      tx = fallbackTransport;
    }
    receiverTx[receiverId] = tx;

    // If there are any early-queued messages, send them now directly through
    // the needed transport. This queue will only have contents if
    // useEarlyQueueing === true (see call method).
    var earlyQueue = earlyRpcQueue[receiverId] || [];
    for (var i = 0; i < earlyQueue.length; ++i) {
      var rpc = earlyQueue[i];
      // There was no auth/rpc token set before, so set it now.
      rpc.t = getAuthToken(receiverId);
      tx.call(receiverId, rpc.f, rpc);
    }

    // Clear the queue so it won't be sent again.
    earlyRpcQueue[receiverId] = [];
  }

  /**
   * Helper function to process an RPC request
   * @param {Object} rpc RPC request object
   * @private
   */
  function process(rpc) {
    //
    // RPC object contents:
    //   s: Service Name
    //   f: From
    //   c: The callback ID or 0 if none.
    //   a: The arguments for this RPC call.
    //   t: The authentication token.
    //
    if (rpc && typeof rpc.s === 'string' && typeof rpc.f === 'string' &&
        rpc.a instanceof Array) {

      // Validate auth token.
      if (authToken[rpc.f]) {
        // We don't do type coercion here because all entries in the authToken
        // object are strings, as are all url params. See setupReceiver(...).
        if (authToken[rpc.f] !== rpc.t) {
          throw new Error("Invalid auth token. " +
              authToken[rpc.f] + " vs " + rpc.t);
        }
      }

      if (rpc.s === ACK) {
        // Acknowledgement API, used to indicate a receiver is ready.
        window.setTimeout(function() { transportReady(rpc.f, true); }, 0);
        return;
      }

      // If there is a callback for this service, attach a callback function
      // to the rpc context object for asynchronous rpc services.
      //
      // Synchronous rpc request handlers should simply ignore it and return a
      // value as usual.
      // Asynchronous rpc request handlers, on the other hand, should pass its
      // result to this callback function and not return a value on exit.
      //
      // For example, the following rpc handler passes the first parameter back
      // to its rpc client with a one-second delay.
      //
      // function asyncRpcHandler(param) {
      //   var me = this;
      //   setTimeout(function() {
      //     me.callback(param);
      //   }, 1000);
      // }
      if (rpc.c) {
        rpc.callback = function(result) {
          gadgets.rpc.call(rpc.f, CALLBACK_NAME, null, rpc.c, result);
        };
      }

      // Call the requested RPC service.
      var result = (services[rpc.s] ||
                    services[DEFAULT_NAME]).apply(rpc, rpc.a);

      // If the rpc request handler returns a value, immediately pass it back
      // to the callback. Otherwise, do nothing, assuming that the rpc handler
      // will make an asynchronous call later.
      if (rpc.c && typeof result !== 'undefined') {
        gadgets.rpc.call(rpc.f, CALLBACK_NAME, null, rpc.c, result);
      }
    }
  }

  /**
   * Helper method returning a canonicalized protocol://host[:port] for
   * a given input URL, provided as a string. Used to compute convenient
   * relay URLs and to determine whether a call is coming from the same
   * domain as its receiver (bypassing the try/catch capability detection
   * flow, thereby obviating Firebug and other tools reporting an exception).
   *
   * @param {string} url Base URL to canonicalize.
   */
  function getOrigin(url) {
    if (!url) {
      return "";
    }
    url = url.toLowerCase();
    if (url.indexOf("//") == 0) {
      url = window.location.protocol + url;
    }
    if (url.indexOf("://") == -1) {
      // Assumed to be schemaless. Default to current protocol.
      url = window.location.protocol + "//" + url;
    }
    // At this point we guarantee that "://" is in the URL and defines
    // current protocol. Skip past this to search for host:port.
    var host = url.substring(url.indexOf("://") + 3);

    // Find the first slash char, delimiting the host:port.
    var slashPos = host.indexOf("/");
    if (slashPos != -1) {
      host = host.substring(0, slashPos);
    }

    var protocol = url.substring(0, url.indexOf("://"));

    // Use port only if it's not default for the protocol.
    var portStr = "";
    var portPos = host.indexOf(":");
    if (portPos != -1) {
      var port = host.substring(portPos + 1);
      host = host.substring(0, portPos);
      if ((protocol === "http" && port !== "80") ||
          (protocol === "https" && port !== "443")) {
        portStr = ":" + port;
      }
    }

    // Return <protocol>://<host>[<port>]
    return protocol + "://" + host + portStr;
  }

  // Pick the most efficient RPC relay mechanism.
  var transport = getTransport();

  // Create the Default RPC handler.
  services[DEFAULT_NAME] = function() {
    gadgets.warn('Unknown RPC service: ' + this.s);
  };

  // Create a Special RPC handler for callbacks.
  services[CALLBACK_NAME] = function(callbackId, result) {
    var callback = callbacks[callbackId];
    if (callback) {
      delete callbacks[callbackId];
      callback(result);
    }
  };

  /**
   * Conducts any frame-specific work necessary to setup
   * the channel type chosen. This method is called when
   * the container page first registers the gadget in the
   * RPC mechanism. Gadgets, in turn, will complete the setup
   * of the channel once they send their first messages.
   */
  function setupFrame(frameId, token) {
    if (setup[frameId] === true) {
      return;
    }

    if (typeof setup[frameId] === 'undefined') {
      setup[frameId] = 0;
    }

    var tgtFrame = document.getElementById(frameId);
    if (frameId === '..' || tgtFrame != null) {
      if (transport.setup(frameId, token) === true) {
        setup[frameId] = true;
        return;
      }
    }

    if (setup[frameId] !== true && setup[frameId]++ < SETUP_FRAME_MAX_TRIES) {
      // Try again in a bit, assuming that frame will soon exist.
      window.setTimeout(function() { setupFrame(frameId, token) },
                        SETUP_FRAME_TIMEOUT);
    } else {
      // Fail: fall back for this gadget.
      receiverTx[frameId] = fallbackTransport;
      setup[frameId] = true;
    }
  }

  /**
   * Attempts to make an rpc by calling the target's receive method directly.
   * This works when gadgets are rendered on the same domain as their container,
   * a potentially useful optimization for trusted content which keeps
   * RPC behind a consistent interface.
   * @param {String} target Module id of the rpc service provider
   * @param {String} from Module id of the caller (this)
   * @param {String} callbackId Id of the call
   * @param {String} rpcData JSON-encoded RPC payload
   * @return
   */
  function callSameDomain(target, rpc) {
    if (typeof sameDomain[target] === 'undefined') {
      // Seed with a negative, typed value to avoid
      // hitting this code path repeatedly.
      sameDomain[target] = false;
      var targetRelay = gadgets.rpc.getRelayUrl(target);
      if (getOrigin(targetRelay) !== getOrigin(window.location.href)) {
        // Not worth trying -- avoid the error and just return.
        return false;
      }

      var targetEl = null;
      if (target === '..') {
        targetEl = window.parent;
      } else {
        targetEl = window.frames[target];
      }
      try {
        // If this succeeds, then same-domain policy applied
        sameDomain[target] = targetEl.gadgets.rpc.receiveSameDomain;
      } catch (e) {
        // Shouldn't happen due to origin check. Caught to emit
        // more meaningful error to the caller.
        gadgets.error("Same domain call failed: parent= incorrectly set.");
      }
    }

    if (typeof sameDomain[target] === 'function') {
      // Call target's receive method
      sameDomain[target](rpc);
      return true;
    }

    return false;
  }

  function setRelayUrl(targetId, url, opt_useLegacy) {
    relayUrl[targetId] = url;
    useLegacyProtocol[targetId] = !!opt_useLegacy;
  }

  function getAuthToken(targetId) {
    return authToken[targetId];
  }

  function setAuthToken(targetId, token) {
    token = token || "";

    // Coerce token to a String, ensuring that all authToken values
    // are strings. This ensures correct comparison with URL params
    // in the process(rpc) method.
    authToken[targetId] = String(token);

    setupFrame(targetId, token);
  }

  function setupContainerGadgetContext(rpctoken) {
    /**
     * Initializes gadget to container RPC params from the provided configuration.
     */
    function init(config) {
      var configRpc = config ? config.rpc : {};
      var parentRelayUrl = configRpc.parentRelayUrl;

      // Allow for wild card parent relay files as long as it's from a
      // white listed domain. This is enforced by the rendering servlet.
      if (parentRelayUrl.substring(0, 7) !== 'http://' &&
          parentRelayUrl.substring(0, 8) !== 'https://' &&
          parentRelayUrl.substring(0, 2) !== '//') {
        // Relative path: we append to the parent.
        // We're relying on the server validating the parent parameter in this
        // case. Because of this, parent may only be passed in the query, not fragment.
        if (typeof params.parent === "string" && params.parent !== "") {
          // Otherwise, relayUrl['..'] will be null, signaling transport
          // code to ignore rpc calls since they cannot work without a
          // relay URL with host qualification.
          if (parentRelayUrl.substring(0, 1) !== '/') {
            // Path-relative. Trust that parent is passed in appropriately.
            var lastSlash = params.parent.lastIndexOf('/');
            parentRelayUrl = params.parent.substring(0, lastSlash + 1) + parentRelayUrl;
          } else {
            // Host-relative.
            parentRelayUrl = getOrigin(params.parent) + parentRelayUrl;
          }
        }
      }

      var useLegacy = !!configRpc.useLegacyProtocol;
      setRelayUrl('..', parentRelayUrl, useLegacy);

      if (useLegacy) {
        transport = gadgets.rpctx.ifpc;
        transport.init(process, transportReady);
      }

      // Sets the auth token and signals transport to setup connection to container.
      setAuthToken('..', rpctoken);
    }

    var requiredConfig = {
      parentRelayUrl : gadgets.config.NonEmptyStringValidator
    };
    gadgets.config.register("rpc", requiredConfig, init);
  }

  function setupContainerGenericIframe(rpctoken, opt_parent) {
    // Generic child IFRAME setting up connection w/ its container.
    // Use the opt_parent param if provided, or the "parent" query param
    // if found -- otherwise, do nothing since this call might be initiated
    // automatically at first, then actively later in IFRAME code.
    var parent = opt_parent || params.parent;
    if (parent) {
      setRelayUrl('..', parent);
      setAuthToken('..', rpctoken);
    }
  }

  function setupChildIframe(gadgetId, opt_frameurl, opt_authtoken) {
    if (!gadgets.util) {
      return;
    }
    var childIframe = document.getElementById(gadgetId);
    if (!childIframe) {
      throw new Error("Cannot set up gadgets.rpc receiver with ID: " + gadgetId +
          ", element not found.");
    }

    // The "relay URL" can either be explicitly specified or is set as
    // the child IFRAME URL verbatim.
    var relayUrl = opt_frameurl || childIframe.src;
    setRelayUrl(gadgetId, relayUrl);

    // The auth token is parsed from child params (rpctoken) or overridden.
    var childParams = gadgets.util.getUrlParameters(childIframe.src);
    var rpctoken = opt_authtoken || childParams.rpctoken;
    setAuthToken(gadgetId, rpctoken);
  }

  function setupReceiver(targetId, opt_receiverurl, opt_authtoken) {
    if (targetId === '..') {
      // Gadget/IFRAME to container.
      var rpctoken = opt_authtoken || params.rpctoken || params.ifpctok || "";
      if (gadgets.config) {
        setupContainerGadgetContext(rpctoken);
      } else {
        setupContainerGenericIframe(rpctoken, opt_receiverurl);
      }
    } else {
      // Container to child.
      setupChildIframe(targetId, opt_receiverurl, opt_authtoken);
    }
  }

  // gadgets.config might not be available, such as when serving container js.
  if (isChild) {
    setupReceiver('..');
  }

  return /** @scope gadgets.rpc */ {
    /**
     * Registers an RPC service.
     * @param {String} serviceName Service name to register.
     * @param {Function} handler Service handler.
     *
     * @member gadgets.rpc
     */
    register: function(serviceName, handler) {
      if (serviceName === CALLBACK_NAME || serviceName === ACK) {
        throw new Error("Cannot overwrite callback/ack service");
      }

      if (serviceName === DEFAULT_NAME) {
        throw new Error("Cannot overwrite default service:"
                        + " use registerDefault");
      }

      services[serviceName] = handler;
    },

    /**
     * Unregisters an RPC service.
     * @param {String} serviceName Service name to unregister.
     *
     * @member gadgets.rpc
     */
    unregister: function(serviceName) {
      if (serviceName === CALLBACK_NAME || serviceName === ACK) {
        throw new Error("Cannot delete callback/ack service");
      }

      if (serviceName === DEFAULT_NAME) {
        throw new Error("Cannot delete default service:"
                        + " use unregisterDefault");
      }

      delete services[serviceName];
    },

    /**
     * Registers a default service handler to processes all unknown
     * RPC calls which raise an exception by default.
     * @param {Function} handler Service handler.
     *
     * @member gadgets.rpc
     */
    registerDefault: function(handler) {
      services[DEFAULT_NAME] = handler;
    },

    /**
     * Unregisters the default service handler. Future unknown RPC
     * calls will fail silently.
     *
     * @member gadgets.rpc
     */
    unregisterDefault: function() {
      delete services[DEFAULT_NAME];
    },

    /**
     * Forces all subsequent calls to be made by a transport
     * method that allows the caller to verify the message receiver
     * (by way of the parent parameter, through getRelayUrl(...)).
     * At present this means IFPC or WPM.
     */
    forceParentVerifiable: function() {
      if (!transport.isParentVerifiable()) {
        transport = gadgets.rpctx.ifpc;
      }
    },

    /**
     * Calls an RPC service.
     * @param {String} targetId Module Id of the RPC service provider.
     *                          Empty if calling the parent container.
     * @param {String} serviceName Service name to call.
     * @param {Function|null} callback Callback function (if any) to process
     *                                 the return value of the RPC request.
     * @param {*} var_args Parameters for the RPC request.
     *
     * @member gadgets.rpc
     */
    call: function(targetId, serviceName, callback, var_args) {
      targetId = targetId || '..';
      // Default to the container calling.
      var from = '..';

      if (targetId === '..') {
        from = rpcId;
      }

      ++callId;
      if (callback) {
        callbacks[callId] = callback;
      }

      var rpc = {
        s: serviceName,
        f: from,
        c: callback ? callId : 0,
        a: Array.prototype.slice.call(arguments, 3),
        t: authToken[targetId],
        l: useLegacyProtocol[targetId]
      };

      // If target is on the same domain, call method directly
      if (callSameDomain(targetId, rpc)) {
        return;
      }

      // Attempt to make call via a cross-domain transport.
      // Retrieve the transport for the given target - if one
      // target is misconfigured, it won't affect the others.
      var channel = receiverTx[targetId] ? receiverTx[targetId] : transport;

      if (!channel) {
        // Not set up yet. Enqueue the rpc for such time as it is.
        if (!earlyRpcQueue[targetId]) {
          earlyRpcQueue[targetId] = [ rpc ];
        } else {
          earlyRpcQueue[targetId].push(rpc);
        }
        return;
      }

      // If we are told to use the legacy format, then we must
      // default to IFPC.
      if (useLegacyProtocol[targetId]) {
        channel = gadgets.rpctx.ifpc;
      }

      if (channel.call(targetId, from, rpc) === false) {
        // Fall back to IFPC. This behavior may be removed as IFPC is as well.
        receiverTx[targetId] = fallbackTransport;
        transport.call(targetId, from, rpc);
      }
    },

    /**
     * Gets the relay URL of a target frame.
     * @param {String} targetId Name of the target frame.
     * @return {String|undefined} Relay URL of the target frame.
     *
     * @member gadgets.rpc
     */
    getRelayUrl: function(targetId) {
      var url = relayUrl[targetId];
      // Some RPC methods (wpm, for one) are unhappy with schemeless URLs.
      if (url && url.indexOf('//') == 0) {
        url = document.location.protocol + url;
      }
      
      return url;
    },

    /**
     * Sets the relay URL of a target frame.
     * @param {String} targetId Name of the target frame.
     * @param {String} url Full relay URL of the target frame.
     * @param {Boolean} opt_useLegacy True if this relay needs the legacy IFPC
     *     wire format.
     *
     * @member gadgets.rpc
     * @deprecated
     */
    setRelayUrl: setRelayUrl,

    /**
     * Sets the auth token of a target frame.
     * @param {String} targetId Name of the target frame.
     * @param {String} token The authentication token to use for all
     *     calls to or from this target id.
     *
     * @member gadgets.rpc
     * @deprecated
     */
    setAuthToken: setAuthToken,

    /**
     * Sets up the gadgets.rpc library to communicate with the receiver.
     * This method replaces setRelayUrl(...) and setAuthToken(...)
     *
     * Simplified instructions - highly recommended:
     * 1. Generate <iframe id="<ID>" url="...#parent=<PARENTURL>&rpctoken=<RANDOM>"/>
     *    and add to DOM.
     * 2. Call gadgets.rpc.setupReceiver("<ID>");
     * --> All parent/child communication initializes automatically from here.
     *     Naturally, both sides need to include the library.
     *
     * Detailed container/parent instructions:
     * 1. Create the target IFRAME (eg. gadget) with a given <ID> and params
     *    rpctoken=<token> (eg. #rpctoken=1234), which is a random/unguessbable
     *    string, and parent=<url>, where <url> is the URL of the container.
     * 2. Append IFRAME to the document.
     * 3. Call gadgets.rpc.setupReceiver(<ID>)
     * [Optional]. Strictly speaking, you may omit rpctoken and parent. This
     *             practice earns little but is occasionally useful for testing.
     *             If you omit parent, you MUST pass your container URL as the 2nd
     *             parameter to this method.
     *
     * Detailed gadget/child IFRAME instructions:
     * 0. If your container/parent passed parent and rpctoken params (query string
     *    or fragment are both OK), you needn't do anything. The library will self-
     *    initialize.
     * 1. If "parent" is omitted, you MUST call this method with targetId '..'
     *    and the second param set to the parent URL.
     * 2. If "rpctoken" is omitted, but the container set an authToken manually
     *    for this frame, you MUST pass that ID (however acquired) as the 2nd param
     *    to this method.
     */
    setupReceiver: setupReceiver,

    /**
     * Helper method to retrieve the authToken for a given gadget.
     * Not to be used directly.
     */
    getAuthToken: getAuthToken,

    /**
     * Gets the RPC relay mechanism.
     * @return {String} RPC relay mechanism. See above for
     *   a list of supported types.
     *
     * @member gadgets.rpc
     */
    getRelayChannel: function() {
      return transport.getCode();
    },

    /**
     * Receives and processes an RPC request. (Not to be used directly.)
     * Only used by IFPC.
     * @param {Array.<String>} fragment An RPC request fragment encoded as
     *        an array. The first 4 elements are target id, source id & call id,
     *        total packet number, packet id. The last element stores the actual
     *        JSON-encoded and URI escaped packet data.
     *
     * @member gadgets.rpc
     * @deprecated
     */
    receive: function(fragment) {
      if (fragment.length > 4) {
        process(gadgets.json.parse(
            decodeURIComponent(fragment[fragment.length - 1])));
      }
    },

    /**
     * Receives and processes an RPC request sent via the same domain.
     * (Not to be used directly). Converts the inbound rpc object's
     * Array into a local Array to pass the process() Array test.
     * @param {Object} rpc RPC object containing all request params
     */
    receiveSameDomain: function(rpc) {
      // Pass through to local process method but converting to a local Array
      rpc.a = Array.prototype.slice.call(rpc.a);
      window.setTimeout(function() { process(rpc); }, 0);
    },

    /**
     * Helper method to get the protocol://host:port of an input URL.
     */
    getOrigin: getOrigin,

    /**
     * Internal-only method used to initialize gadgets.rpc.
     */
    init: function() {
      // Conduct any global setup necessary for the chosen transport.
      // Do so after gadgets.rpc definition to allow transport to access
      // gadgets.rpc methods.
      if (transport.init(process, transportReady) === false) {
        transport = fallbackTransport;
      }
    },

    /** Exported constant, for use by transports only. */
    ACK: ACK,

    RPC_ID: rpcId
  };
}();

// Initialize library/transport.
gadgets.rpc.init();
