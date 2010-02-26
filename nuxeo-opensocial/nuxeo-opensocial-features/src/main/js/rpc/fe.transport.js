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

var gadgets = gadgets || {};
gadgets.rpctx = gadgets.rpctx || {};

/*
 * For Gecko-based browsers, the security model allows a child to call a
 * function on the frameElement of the iframe, even if the child is in
 * a different domain. This method is dubbed "frameElement" (fe).
 *
 * The ability to add and call such functions on the frameElement allows
 * a bidirectional channel to be setup via the adding of simple function
 * references on the frameElement object itself. In this implementation,
 * when the container sets up the authentication information for that gadget
 * (by calling setAuth(...)) it as well adds a special function on the
 * gadget's iframe. This function can then be used by the gadget to send
 * messages to the container. In turn, when the gadget tries to send a
 * message, it checks to see if this function has its own function stored
 * that can be used by the container to call the gadget. If not, the
 * function is created and subsequently used by the container.
 * Note that as a result, FE can only be used by a container to call a
 * particular gadget *after* that gadget has called the container at
 * least once via FE.
 *
 *   fe: Gecko-specific frameElement trick.
 *      - Firefox 1+
 */
gadgets.rpctx.frameElement = function() {
  // Consts for FrameElement.
  var FE_G2C_CHANNEL = '__g2c_rpc';
  var FE_C2G_CHANNEL = '__c2g_rpc';
  var process;
  var ready;

  function callFrameElement(targetId, from, rpc) {
    try {
      if (from !== '..') {
        // Call from gadget to the container.
        var fe = window.frameElement;

        if (typeof fe[FE_G2C_CHANNEL] === 'function') {
          // Complete the setup of the FE channel if need be.
          if (typeof fe[FE_G2C_CHANNEL][FE_C2G_CHANNEL] !== 'function') {
            fe[FE_G2C_CHANNEL][FE_C2G_CHANNEL] = function(args) {
              process(gadgets.json.parse(args));
            };
          }

          // Conduct the RPC call.
          fe[FE_G2C_CHANNEL](gadgets.json.stringify(rpc));
          return;
        }
      } else {
        // Call from container to gadget[targetId].
        var frame = document.getElementById(targetId);

        if (typeof frame[FE_G2C_CHANNEL] === 'function' &&
            typeof frame[FE_G2C_CHANNEL][FE_C2G_CHANNEL] === 'function') {

          // Conduct the RPC call.
          frame[FE_G2C_CHANNEL][FE_C2G_CHANNEL](gadgets.json.stringify(rpc));
          return;
        }
      }
    } catch (e) {
    }
    return true;
  }

  return {
    getCode: function() {
      return 'fe';
    },

    isParentVerifiable: function() {
      return false;
    },
  
    init: function(processFn, readyFn) {
      // No global setup.
      process = processFn;
      ready = readyFn;
      return true;
    },

    setup: function(receiverId, token) {
      // Indicate OK to call to container. This will be true
      // by the end of this method.
      if (receiverId !== '..') {
        try {
          var frame = document.getElementById(receiverId);
          frame[FE_G2C_CHANNEL] = function(args) {
            process(gadgets.json.parse(args));
          };
        } catch (e) {
          return false;
        }
      }
      if (receiverId === '..') {
        ready('..', true);
        var ackFn = function() {
          window.setTimeout(function() {
            gadgets.rpc.call(receiverId, gadgets.rpc.ACK)
          }, 500);
        }
        // Setup to container always happens before onload.
        // If it didn't, the correct fix would be in gadgets.util.
        gadgets.util.registerOnLoadHandler(ackFn);
      }
      return true;
    },

    call: function(targetId, from, rpc) {
      callFrameElement(targetId, from, rpc);
    } 

  };
}();
