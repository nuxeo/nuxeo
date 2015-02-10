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

  m.addFromListTemplate = function(templateElement) {
    var tel = jQuery(templateElement),
        count = templateElement.siblings('.listItem').length;

    // unescape our template's html content
    var text = jQuery('<textarea/>').html(tel.html()).val();
    // replace our marker with the row index
    text = text.trim().replace(/TEMPLATE_INDEX_MARKER/g, count);
    // parse the html (including scripts)
    var el = jQuery.parseHTML(text, document, true);
    // place in the DOM
    tel.before(el);
    return false;
  };

  m.deleteFromList = function(rowElement) {
    rowElement.remove();
    return false;
  };

  m.moveUpList = function(rowElement) {
    rowElement.insertBefore(rowElement.prev());
    return false;
  };

  m.moveDownList = function(rowElement) {
    rowElement.insertAfter(rowElement.next());
    return false;
  };

  return m

}(nuxeo.utils || {}));
