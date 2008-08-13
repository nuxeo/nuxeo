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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JSonDocTreeSerializer implements TreeItemVisitor {

    protected WebContext ctx;

    public JSonDocTreeSerializer(WebContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Must be overrided to provide real URLs
     */
    public String getUrl(DocumentModel doc) {
        return ctx.getUrlPath(doc);
    }

    public JSONArray toJSON(Collection<TreeItem> items) {
        JSONArray ar = new JSONArray();
        for (TreeItem item : items) {
            ar.add(toJSON(item));
        }
        return ar;
    }

    public JSONArray toJSON(TreeItem ... items) {
        JSONArray ar = new JSONArray();
        for (TreeItem item : items) {
            ar.add(toJSON(item));
        }
        return ar;
    }

    public JSONObject toJSON(TreeItem root) {
        return (JSONObject)root.accept(this);
    }

    protected JSONObject toJSON(DocumentModel doc) {
        JSONObject json = new JSONObject();
        json.element("text", doc.getName())
            .element("id", doc.getPathAsString())
            .element("href", getUrl(doc));
        if (doc.isFolder()) {
            json.element("hasChildren", true);
        }
        return json;
    }

    public Object visit(TreeItem item) {
        DocumentModel doc = (DocumentModel)item.getObject();
        JSONObject json = toJSON(doc);
        if (item.isExpanded()) {
            json.element("expanded", true);
            TreeItem[] children = item.getChildren();
            if (children != null && children.length > 0) {
                JSONArray jsons = new JSONArray();
                for (TreeItem child : children) {
                    JSONObject childJson = (JSONObject)visit(child);
                    jsons.add(childJson);
                }
                json.element("children", jsons);
            }
        }
        return json;
    }

}
