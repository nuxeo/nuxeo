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
 * @fileoverview
 * The global object gadgets.json contains two methods.
 *
 * gadgets.json.stringify(value) takes a JavaScript value and produces a JSON
 * text. The value must not be cyclical.
 *
 * gadgets.json.parse(text) takes a JSON text and produces a JavaScript value.
 * It will return false if there is an error.
*/

var gadgets = gadgets || {};

/**
 * @static
 * @class Provides operations for translating objects to and from JSON.
 * @name gadgets.json
 */

/**
 * Port of the public domain JSON library by Douglas Crockford.
 * See: http://www.json.org/json2.js
 */
if (window.JSON && window.JSON.parse && window.JSON.stringify) {
  // HTML5 implementation, or already defined.
  // Not a direct alias as the opensocial specification disagrees with the HTML5 JSON spec.
  // JSON says to throw on parse errors and to support filtering functions. OS does not.
  gadgets.json = {
    parse: function(str) {
      try {
        return window.JSON.parse(str);
      } catch (e) {
        return false;
      }
    },
    stringify: function(obj) {
      try {
        return window.JSON.stringify(obj);
      } catch (e) {
        return null;
      }
    }
  };
} else {
  gadgets.json = function () {
  
    /**
     * Formats integers to 2 digits.
     * @param {Number} n
     */
    function f(n) {
      return n < 10 ? '0' + n : n;
    }
  
    Date.prototype.toJSON = function () {
      return [this.getUTCFullYear(), '-',
             f(this.getUTCMonth() + 1), '-',
             f(this.getUTCDate()), 'T',
             f(this.getUTCHours()), ':',
             f(this.getUTCMinutes()), ':',
             f(this.getUTCSeconds()), 'Z'].join("");
    };
  
    // table of character substitutions
    var m = {
      '\b': '\\b',
      '\t': '\\t',
      '\n': '\\n',
      '\f': '\\f',
      '\r': '\\r',
      '"' : '\\"',
      '\\': '\\\\'
    };
  
    /**
     * Converts a json object into a string.
     */
    function stringify(value) {
      var a,          // The array holding the partial texts.
          i,          // The loop counter.
          k,          // The member key.
          l,          // Length.
          r = /["\\\x00-\x1f\x7f-\x9f]/g,
          v;          // The member value.
  
      switch (typeof value) {
      case 'string':
      // If the string contains no control characters, no quote characters, and no
      // backslash characters, then we can safely slap some quotes around it.
      // Otherwise we must also replace the offending characters with safe ones.
        return r.test(value) ?
            '"' + value.replace(r, function (a) {
              var c = m[a];
              if (c) {
                return c;
              }
              c = a.charCodeAt();
              return '\\u00' + Math.floor(c / 16).toString(16) +
                  (c % 16).toString(16);
              }) + '"' : '"' + value + '"';
      case 'number':
      // JSON numbers must be finite. Encode non-finite numbers as null.
        return isFinite(value) ? String(value) : 'null';
      case 'boolean':
      case 'null':
        return String(value);
      case 'object':
      // Due to a specification blunder in ECMAScript,
      // typeof null is 'object', so watch out for that case.
        if (!value) {
          return 'null';
        }
        // toJSON check removed; re-implement when it doesn't break other libs.
        a = [];
        if (typeof value.length === 'number' &&
            !value.propertyIsEnumerable('length')) {
          // The object is an array. Stringify every element. Use null as a
          // placeholder for non-JSON values.
          l = value.length;
          for (i = 0; i < l; i += 1) {
            a.push(stringify(value[i]) || 'null');
          }
          // Join all of the elements together and wrap them in brackets.
          return '[' + a.join(',') + ']';
        }
        // Otherwise, iterate through all of the keys in the object.
        for (k in value) {
          if (k.match('___$'))
            continue;
          if (value.hasOwnProperty(k)) {
            if (typeof k === 'string') {
              v = stringify(value[k]);
              if (v) {
                a.push(stringify(k) + ':' + v);
              }
            }
          }
        }
        // Join all of the member texts together and wrap them in braces.
        return '{' + a.join(',') + '}';
      }
    }
  
    return {
      stringify: stringify,
      parse: function (text) {
      // Parsing happens in three stages. In the first stage, we run the text against
      // regular expressions that look for non-JSON patterns. We are especially
      // concerned with '()' and 'new' because they can cause invocation, and '='
      // because it can cause mutation. But just to be safe, we want to reject all
      // unexpected forms.
      
      // We split the first stage into 4 regexp operations in order to work around
      // crippling inefficiencies in IE's and Safari's regexp engines. First we
      // replace all backslash pairs with '@' (a non-JSON character). Second, we
      // replace all simple value tokens with ']' characters. Third, we delete all
      // open brackets that follow a colon or comma or that begin the text. Finally,
      // we look to see that the remaining characters are only whitespace or ']' or
      // ',' or ':' or '{' or '}'. If that is so, then the text is safe for eval.
  
        if (/^[\],:{}\s]*$/.test(text.replace(/\\["\\\/b-u]/g, '@').
            replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g, ']').
            replace(/(?:^|:|,)(?:\s*\[)+/g, ''))) {
          return eval('(' + text + ')');
        }
        // If the text is not JSON parseable, then return false.
  
        return false;
      }
    };
  }();
}

