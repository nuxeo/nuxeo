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

/**
 * @fileoverview This library augments gadets.window with functionality to set
 *               the title of a gadget dynamically.
 */

var gadgets = gadgets || {};

gadgets.nuxeo = gadgets.nuxeo || {};

/**
 * Gets the Id of the enclosing space doc id
 *
 * @scope gadgets.nuxeo
 */
gadgets.nuxeo.getSpaceId = function(callback) {
  gadgets.rpc.call(null, "get_nuxeo_space_id", callback, null);
};

gadgets.nuxeo.getHtmlContent = function(callback) {
  gadgets.rpc.call(null, "get_htmlcontent", callback, null);
};

gadgets.nuxeo.setHtmlContent = function(htmlContent, callback) {
  gadgets.rpc.call(null, "set_htmlcontent", callback, htmlContent);
};

gadgets.nuxeo.getGadgetId = function(callback) {
  gadgets.rpc.call(null, "get_nuxeo_gadget_id", callback, null);
};

gadgets.nuxeo.maximizeGadget = function(view, callback) {
  gadgets.rpc.call(null, "maximize", callback, view);
};

gadgets.nuxeo.minimizeGadget = function(callback) {
  gadgets.rpc.call(null, "minimize", callback, null);
};