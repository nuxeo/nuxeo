/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume</a>
 */

var nuxeo = nuxeo || {};

nuxeo.utils = (function(m) {

  var eventListeners = [];

  var entityMap = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
    '/': '&#x2F;'
  };

  m.escapeHTML = function escapeHTML(string) {
    return String(string).replace(/[&<>"'\/]/g, function fromEntityMap(s) { return entityMap[s]; });
  };

  m.addOnEvent = function addOnEvent(callback) {
    if (typeof callback === 'function') {
        eventListeners[eventListeners.length] = callback;
    } else {
        throw new Error("nuxeo.utils.addOnEvent: Added a callback that was not a function");
    }
  };

  function executeEventListeners(data) {
    for (var i in eventListeners) {
      if (eventListeners.hasOwnProperty(i)) {
        eventListeners[i].call(null, data);
      }
    }
  }

  function execute(func, flag) {
    executeEventListeners({"status": "begin", "flag": flag});
    func();
    executeEventListeners({"status": "success", "flag": flag});
  }

  function fixTinyMCE(el) {
    jQuery(el).find("textarea.mceEditor").each(function() { resetTinyMCE(this.id); });
  }

  m.moreLessTableRows = function(eltId, displayAll, displayLimit) {
    var itemTable = jQuery("[id$='" + eltId + "']");
    var items = jQuery("[id$='" + eltId + "'] tr");
    var moreLink = itemTable.parent().find(".nx-more");
    var lessLink = itemTable.parent().find(".nx-less");
    if (items.length <= displayLimit) {
      moreLink.css("display", "none");
      lessLink.css("display", "none");
      return;
    } else {
      var itemsToHideDisplay = itemTable.find("tr:gt(" + (displayLimit - 1) + ")");
      if (displayAll) {
        moreLink.css("display", "none");
        lessLink.css("display", "");
        itemsToHideDisplay.css("display", "");
      } else {
        lessLink.css("display", "none");
        moreLink.css("display", "");
        itemsToHideDisplay.css("display", "none");
      }
    }
  };

  m.select2ifySelect = function(eltId, params) {
    jQuery(document.getElementById(eltId)).select2(params);
  };

  m.addFromListTemplate = function(parentId, templateElement) {
    execute(function() {
      var tel = jQuery(templateElement),
          count = templateElement.siblings('.listItem').length;

      // unescape our template's html content
      var text = jQuery('<div/>').html(tel.html()).text().trim();
      // replace the hidden input name, removing the index marker to get a list param
      var re = new RegExp(parentId + ":TEMPLATE_INDEX_MARKER:rowIndex", "g");
      text = text.replace(re, parentId + ':rowIndex');
      // replace our marker with the row index
      re = new RegExp(parentId + ":TEMPLATE_INDEX_MARKER", "g");
      text = text.replace(re, parentId + ':' + count);
      // parse the html (including scripts)
      var el = jQuery.parseHTML(text, document, true);
      // make sure hidden input value is also replaced
      jQuery(el).find("input[value='TEMPLATE_INDEX_MARKER']").val(count);
      // place in the DOM
      tel.before(el);
      fixTinyMCE(el);
    }, "js-list-add");
    return false;
  };

  m.deleteFromList = function(rowElement) {
    execute(function(){rowElement.remove();}, "js-list-delete");
    return false;
  };

  m.moveUpList = function(rowElement) {
    execute(function() {
      rowElement.insertBefore(rowElement.prev());
      fixTinyMCE(rowElement);
    }, "js-list-move-up");
    return false;
  };

  m.moveDownList = function(rowElement) {
    execute(function() {
      rowElement.insertAfter(rowElement.next());
      fixTinyMCE(rowElement);
    }, "js-list-move-down");
    return false;
  };

  return m

}(nuxeo.utils || {}));
