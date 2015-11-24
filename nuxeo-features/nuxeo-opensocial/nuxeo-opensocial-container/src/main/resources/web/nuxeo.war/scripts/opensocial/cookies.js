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
 * @fileoverview Functions for setting, getting and deleting cookies
 */

/**
 * Namespace for cookie functions
 */

// TODO: find the official solution for a cookies library
var shindig = shindig || {};
shindig.cookies = shindig.cookies || {};


shindig.cookies.JsType_ = {
  UNDEFINED: 'undefined'
};

shindig.cookies.isDef = function(val) {
  return typeof val != shindig.cookies.JsType_.UNDEFINED;
};


/**
 * Sets a cookie.
 * The max_age can be -1 to set a session cookie. To remove and expire cookies,
 * use remove() instead.
 *
 * @param {string} name The cookie name.
 * @param {string} value The cookie value.
 * @param {number} opt_maxAge The max age in seconds (from now). Use -1 to set
 *                            a session cookie. If not provided, the default is
 *                            -1 (i.e. set a session cookie).
 * @param {string} opt_path The path of the cookie, or null to not specify a
 *                          path attribute (browser will use the full request
 *                          path). If not provided, the default is '/' (i.e.
 *                          path=/).
 * @param {string} opt_domain The domain of the cookie, or null to not specify
 *                            a domain attribute (browser will use the full
 *                            request host name). If not provided, the default
 *                            is null (i.e. let browser use full request host
 *                            name).
 */
shindig.cookies.set = function(name, value, opt_maxAge, opt_path, opt_domain) {
  // we do not allow '=' or ';' in the name
  if (/;=/g.test(name)) {
    throw new Error('Invalid cookie name "' + name + '"');
  }
  // we do not allow ';' in value
  if (/;/g.test(value)) {
    throw new Error('Invalid cookie value "' + value + '"');
  }

  if (!shindig.cookies.isDef(opt_maxAge)) {
    opt_maxAge = -1;
  }

  var domainStr = opt_domain ? ';domain=' + opt_domain : '';
  var pathStr = opt_path ? ';path=' + opt_path : '';

  var expiresStr;

  // Case 1: Set a session cookie.
  if (opt_maxAge < 0) {
    expiresStr = '';

  // Case 2: Expire the cookie.
  // Note: We don't tell people about this option in the function doc because
  // we prefer people to use ExpireCookie() to expire cookies.
  } else if (opt_maxAge === 0) {
    // Note: Don't use Jan 1, 1970 for date because NS 4.76 will try to convert
    // it to local time, and if the local time is before Jan 1, 1970, then the
    // browser will ignore the Expires attribute altogether.
    var pastDate = new Date(1970, 1 /*Feb*/, 1);  // Feb 1, 1970
    expiresStr = ';expires=' + pastDate.toUTCString();

  // Case 3: Set a persistent cookie.
  } else {
    var futureDate = new Date((new Date).getTime() + opt_maxAge * 1000);
    expiresStr = ';expires=' + futureDate.toUTCString();
  }

  document.cookie = name + '=' + value + domainStr + pathStr + expiresStr;
};


/**
 * Returns the value for the first cookie with the given name
 * @param {string} name The name of the cookie to get
 * @param {string} opt_default If not found this is returned instead.
 * @return {string|undefined} The value of the cookie. If no cookie is set this
 *                            returns opt_default or undefined if opt_default is
 *                            not provided.
 */
shindig.cookies.get = function(name, opt_default) {
  var nameEq = name + "=";
  var cookie = String(document.cookie);
  for (var pos = -1; (pos = cookie.indexOf(nameEq, pos + 1)) >= 0;) {
    var i = pos;
    // walk back along string skipping whitespace and looking for a ; before
    // the name to make sure that we don't match cookies whose name contains
    // the given name as a suffix.
    while (--i >= 0) {
      var ch = cookie.charAt(i);
      if (ch == ';') {
        i = -1;  // indicate success
        break;
      }
    }
    if (i == -1) {  // first cookie in the string or we found a ;
      var end = cookie.indexOf(';', pos);
      if (end < 0) {
        end = cookie.length;
      }
      return cookie.substring(pos + nameEq.length, end);
    }
  }
  return opt_default;
};


/**
 * Removes and expires a cookie.
 *
 * @param {string} name The cookie name.
 * @param {string} opt_path The path of the cookie, or null to expire a cookie
 *                          set at the full request path. If not provided, the
 *                          default is '/' (i.e. path=/).
 * @param {string} opt_domain The domain of the cookie, or null to expire a
 *                            cookie set at the full request host name. If not
 *                            provided, the default is null (i.e. cookie at
 *                            full request host name).
 */
shindig.cookies.remove = function(name, opt_path, opt_domain) {
  var rv = shindig.cookies.containsKey(name);
  shindig.cookies.set(name, '', 0, opt_path, opt_domain);
  return rv;
};


/**
 * Gets the names and values for all the cookies
 * @private
 * @return {Object} An object with keys and values
 */
shindig.cookies.getKeyValues_ = function() {
  var cookie = String(document.cookie);
  var parts = cookie.split(/\s*;\s*/);
  var keys = [], values = [], index, part;
  for (var i = 0; part = parts[i]; i++) {
    index = part.indexOf('=');

    if (index == -1) { // empty name
      keys.push('');
      values.push(part);
    } else {
      keys.push(part.substring(0, index));
      values.push(part.substring(index + 1));
    }
  }
  return {keys: keys, values: values};
};


/**
 * Gets the names for all the cookies
 * @return {Array} An array with the names of the cookies
 */
shindig.cookies.getKeys = function() {
  return shindig.cookies.getKeyValues_().keys;
};


/**
 * Gets the values for all the cookies
 * @return {Array} An array with the values of the cookies
 */
shindig.cookies.getValues = function() {
  return shindig.cookies.getKeyValues_().values;
};


/**
 * Whether there are any cookies for this document
 * @return {boolean}
 */
shindig.cookies.isEmpty = function() {
  return document.cookie === '';
};


/**
 * Returns the number of cookies for this document
 * @return {number}
 */
shindig.cookies.getCount = function() {
  var cookie = String(document.cookie);
  if (cookie === '') {
    return 0;
  }
  var parts = cookie.split(/\s*;\s*/);
  return parts.length;
};


/**
 * Returns whether there is a cookie with the given name
 * @param {string} key The name of the cookie to test for
 * @return {boolean}
 */
shindig.cookies.containsKey = function(key) {
  var sentinel = {};
  // if get does not find the key it returns the default value. We therefore
  // compare the result with an object to ensure we do not get any false
  // positives.
  return shindig.cookies.get(key, sentinel) !== sentinel;
};


/**
 * Returns whether there is a cookie with the given value. (This is an O(n)
 * operation.)
 * @param {string} value The value to check for
 * @return {boolean}
 */
shindig.cookies.containsValue = function(value) {
  // this O(n) in any case so lets do the trivial thing.
  var values = shindig.cookies.getKeyValues_().values;
  for (var i = 0; i < values.length; i++) {
    if (values[i] == value) {
      return true;
    }
  }
  return false;
};


/**
 * Removes all cookies for this document
 */
shindig.cookies.clear = function() {
  var keys = shindig.cookies.getKeyValues_().keys;
  for (var i = keys.length - 1; i >= 0; i--) {
    shindig.cookies.remove(keys[i]);
  }
};

/**
 * Static constant for the size of cookies. Per the spec, there's a 4K limit
 * to the size of a cookie. To make sure users can't break this limit, we
 * should truncate long cookies at 3950 bytes, to be extra careful with dumb
 * browsers/proxies that interpret 4K as 4000 rather than 4096
 * @type number
 */
shindig.cookies.MAX_COOKIE_LENGTH = 3950;
