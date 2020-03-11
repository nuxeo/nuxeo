/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        return (JSONObject) root.accept(this);
    }

    @Override
    public Object visit(TreeItem item) {
        JSONArray jsons = null;
        if (item.isExpanded()) {
            TreeItem[] children = item.getChildren();
            if (children != null && children.length > 0) {
                jsons = new JSONArray();
                for (TreeItem child : children) {
                    JSONObject childJson = (JSONObject) visit(child);
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
        json.element("text", item.getLabel()).element("id", item.getPath().toString()).element("href", getUrl(item));
        json.element("expanded", item.isExpanded());
        if (item.isContainer()) {
            if (item.isContainer()) {
                if (item.hasChildren()) {
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
