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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class JSonTree {

    protected TreeModelImpl tree;

    public TreeModel getTree() {
        return tree;
    }

    protected abstract Object getInput(WebContext ctx);
    protected abstract ContentProvider  getProvider(WebContext ctx);
    protected abstract JSonTreeSerializer getSerializer(WebContext ctx);


    public String updateSelection(WebContext ctx) {
        return updateSelection(ctx, getProvider(ctx), getSerializer(ctx));
    }

    public String enter(WebContext ctx, String path) {
        return enter(ctx, path, getSerializer(ctx));
    }


    /**
    root=ID   - enter node ID
    toggle=ID - toggle expanded state for node ID
    */
    public synchronized String updateSelection(WebContext ctx, ContentProvider provider, JSonTreeSerializer serializer) {
        try {
            tree.setContentProvider(provider);
            if (!tree.hasInput()) {
                tree.setInput(getInput(ctx));
            }
            FormData form = ctx.getForm();
            String selection = form.getString("root");
            if (selection == null) {
                selection = form.getString("toggle");
                if (selection != null) {
                    toggle(selection);
                }
            } else {
                String result = null;
                if ( "source".equals(selection)){
                    result = enter(ctx, tree.root.getPath().toString(), serializer);
                } else {
                    result = enter(ctx, selection, serializer);
                }
                if (result != null) {
                    return result;
                } else {
                    ctx.getLog().warn("TreeItem: "+selection+" not found");
                }
            }
        } finally {
            tree.setContentProvider(null);
        }
        return null;
    }

    public String getTreeAsJSONArray(WebContext ctx) {
        JSonTreeSerializer serializer = getSerializer(ctx);
        JSONObject o = serializer.toJSON(tree.root);
        JSONArray array = new JSONArray();
        array.add(o);
        return array.toString();
    }

    protected String enter(WebContext ctx, String path, JSonTreeSerializer serializer) {
        TreeItem item = tree.findAndReveal(path);
        if (item != null) {
            item.expand();
            JSONArray result = new JSONArray();
            if (item.isContainer()) {
                result = serializer.toJSON(item.getChildren());
            }
            return result.toString();
        } else {
            return null;
        }
    }

    protected void toggle(String path) {
        TreeItem item = tree.findAndReveal(path);
        if (item.isExpanded()) {
            item.collapse();
        } else {
            item.expand();
        }
    }

}
