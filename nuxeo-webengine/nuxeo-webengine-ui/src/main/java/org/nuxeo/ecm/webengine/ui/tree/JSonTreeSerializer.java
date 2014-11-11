/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.ui.tree;

import java.util.Collection;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JSonTreeSerializer implements TreeItemVisitor {

    /**
     * Must be overridden to provide real URLs
     */
    public String getUrl(TreeItem item) {
        return item.getPath().toString();
    }

    public JSONArray toJSON(Collection<TreeItem> items) {
        JSONArray ar = new JSONArray();
        for (TreeItem item : items) {
            ar.add(toJSON(item));
        }
        return ar;
    }

    public JSONArray toJSON(TreeItem[] items) {
        JSONArray ar = new JSONArray();
        for (TreeItem item : items) {
            ar.add(toJSON(item));
        }
        return ar;
    }

    public JSONObject toJSON(TreeItem root) {
        return (JSONObject)root.accept(this);
    }

    public Object visit(TreeItem item) {
        JSONArray jsons = null;
        if (item.isExpanded()) {
            TreeItem[] children = item.getChildren();
            if (children != null && children.length > 0) {
                jsons = new JSONArray();
                for (TreeItem child : children) {
                    JSONObject childJson = (JSONObject)visit(child);
                    jsons.add(childJson);
                }
            }
        }
        return item2JSON(item, jsons);
    }

    /**
     * You may override this method to change the output JSON.
     */
    protected JSONObject item2JSON(TreeItem item, JSONArray children) {
        JSONObject json = new JSONObject();
        json.element("text", item.getLabel())
            .element("id", item.getPath().toString())
            .element("href", getUrl(item));
        json.element("expanded", item.isExpanded());
        if ( item.isContainer() ){
            if (item.isContainer()) {
                if ( item.hasChildren()) {
                    json.element("children", children);
                } else {
                    json.element("hasChildren", true);
                }
            } else {
                json.element("hasChildren", false);
            }
        }
        return json;
    }

}
