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
 * @fileoverview Caja is a whitelisting javascript sanitizing rewriter.
 * This file tames the APIs that are exposed to a gadget
 */

var caja___ = (function() {
    // URI policy: Rewrites all uris in a cajoled gadget
  var uriCallback = {
    rewrite: function rewrite(uri, mimeTypes) {
      uri = String(uri);
      // By default, only allow references to anchors.
      if (/^#/.test(uri)) {
        return '#' + encodeURIComponent(decodeURIComponent(uri.substring(1)));
        // and files on the same host
      } else if (/^\/(?:[^\/][^?#]*)?$/.test(uri)) {
        return encodeURI(decodeURI(uri));
      }
      // This callback can be replaced with one that passes the URL through
      // a proxy that checks the mimetype.
      return null;
    }
  };

  var fire = function(globalScope) {
    for (var tamer in tamings___) {
      if (tamings___.hasOwnProperty(tamer)) {
        // This is just tamings___[tamer](globalScope)
        // but in a way that does not leak a potent "this"
        // to the taming functions
        (1, tamings___[tamer])(globalScope);
      }
    }
  }
  function whitelistCtors(schemas) {
    var length = schemas.length;
    for (var i=0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0][schema[1]] === 'function') {
        ___.markCtor(schema[0][schema[1]] /* func */, schema[2] /* parent */, schema[1] /* name */);
      } else {
        gadgets.warn("Error taming constructor: " + schema[0] + "." + schema[1]);
      }
    }
  }
  function whitelistFuncs(schemas) {
    var length = schemas.length;
    for (var i=0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0][schema[1]] === 'function') {
        ___.markInnocent(schema[0][schema[1]], schema[1]);
      } else {
        gadgets.warn("Error taming function: " + schema[0] + "." + schema[1]);
      }
    }
  }
  function whitelistMeths(schemas) {
    var length = schemas.length;
    for (var i=0; i < length; i++) {
      var schema = schemas[i];
      if (typeof schema[0][schema[1]] == 'function') {
        ___.grantInnocentMethod(schema[0].prototype, schema[1]);
      } else {
        gadgets.warn("Error taming method: " + schema[0] + "." + schema[1]);
      }
    }
  }

  function enable() {
    var imports = ___.copy(___.sharedImports);
    imports.outers = imports;
    
    var gadgetRoot = document.getElementById('cajoled-output');
    gadgetRoot.className = 'g___';
    document.body.appendChild(gadgetRoot);
    
    imports.htmlEmitter___ = new HtmlEmitter(gadgetRoot);
    attachDocumentStub('-g___', uriCallback, imports, gadgetRoot);
    
    imports.$v = valijaMaker.CALL___(imports.outers);
    
    ___.getNewModuleHandler().setImports(imports);
    
    fire(imports);
    
    imports.outers.gadgets = ___.tame(window.gadgets);
    imports.outers.opensocial = ___.tame(window.opensocial);
    ___.grantRead(imports.outers, 'gadgets');
    ___.grantRead(imports.outers, 'opensocial');
  }
  return {
    enable: enable,
    whitelistCtors: whitelistCtors,
    whitelistFuncs: whitelistFuncs,
    whitelistMeths: whitelistMeths
  };
})();

// Expose alert and console.log to cajoled programs
var tamings___ = tamings___ || [];
tamings___.push(function(imports) {
  imports.outers.alert = function(msg) { alert(msg); };
  ___.grantFunc(imports.outers, 'alert');
});
