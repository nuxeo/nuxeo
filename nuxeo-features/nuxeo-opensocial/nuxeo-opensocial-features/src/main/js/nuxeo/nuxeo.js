/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

var gadgets = gadgets || {};

gadgets.nuxeo = gadgets.nuxeo || {};

gadgets.nuxeo.URLBASE = top.nxContextPath + "/site/gadgetDocumentAPI/";
gadgets.nuxeo.HTMLACTION = "/html";
gadgets.nuxeo.FILEACTION =  "/file";


/**
 * Get the url for the gadget's Resource
 *
 * @scope gadgets.nuxeo
 */
gadgets.nuxeo.getResourceUrl = function(id) {
  return gadgets.nuxeo.URLBASE + id;
}


/**
* Get the url for the gadget's HTML action
*
* @scope gadgets.nuxeo
*/
gadgets.nuxeo.getHtmlActionUrl = function(id) {
  return this.getResourceUrl(id) + this.HTMLACTION;
}

/**
* Get the url for the gadget's Form action
*
* @scope gadgets.nuxeo
*/
gadgets.nuxeo.getFormActionUrl = function(id) {
  return this.getResourceUrl(id);
}

gadgets.nuxeo.getFileActionUrl = function(id) {
  return this.getResourceUrl(id) + this.FILEACTION;
}

/**
 * Returns the current gadget's id
 */
gadgets.nuxeo.getGadgetId = function() {
  return window.name.split("-").slice(2).join("-");
};

gadgets.nuxeo.hasPermission = function(permissionName) {
  var tmp = gadgets.util.getUrlParameters().permission;
  var perms = tmp.substring(1, tmp.length-1).split(",");

  if (perms.indexOf("Everything")>=0) {
    return true;
  }

  var b = false;
  jQuery.each(perms, function(i, p){
    if(jQuery.trim(p) == jQuery.trim(permissionName)) {
      b = true;
      return;
    }
  });
  return b;
};

gadgets.nuxeo.isEditable = function() {
  return gadgets.nuxeo.hasPermission("Write");
};

gadgets.nuxeo.getNXIDPreference = function(name, id) {
  return {"NXID": id, "NXNAME":name};
};

gadgets.nuxeo.refreshGadget = function(){
  gadgets.rpc.call("", "refresh", null, "");
};



