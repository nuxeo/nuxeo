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
 * Get the url to get a file with a specific name
 */
gadgets.nuxeo.getSpecificFileUrl = function(id, specificName) {
  return this.getResourceUrl(id) + this.FILEACTION + "/" + specificName;
}

/**
 * Returns the current gadget's id
 */
gadgets.nuxeo.getGadgetId = function() {
  return window.name.split("-").slice(2).join("-");
};

gadgets.nuxeo.hasPermission = function(permissionName) {
  return this.isEditable();
};

gadgets.nuxeo.isEditable = function() {
  return gadgets.util.getUrlParameters().permission == "1";
};

gadgets.nuxeo.getNXIDPreference = function(name, id) {
  return {"NXID": id, "NXNAME":name};
};

gadgets.nuxeo.refreshGadget = function(){
  gadgets.rpc.call("", "refresh", null, "");
};

