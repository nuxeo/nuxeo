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

import java.io.IOException;

import net.sf.json.JSONArray;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.login.UserSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JSonDocTree {

    protected String id;
    protected String root;
    protected DocumentTree tree;
    protected WebContext ctx;

    public JSonDocTree(WebContext ctx, String id) throws WebException {
        this (ctx, id, "/");
    }

    public JSonDocTree(WebContext ctx, String id, String root) throws WebException {
        this.id = id;
        this.root = root;
        this.ctx = ctx;
    }

    /**
     * @return the root.
     */
    public String getRoot() {
        return root;
    }

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }



    protected void loadTree() throws WebException {
        if (tree == null) {
            UserSession us = ctx.getUserSession();
            tree = (DocumentTree)us.get(id);
            if (tree == null) {
                tree = new DocumentTree(ctx.getCoreSession());
                try {
                    this.tree.setInput(root);
                } catch (ClientException e) {
                    throw WebException.wrap(e);
                }
                us.put(id, tree);
            }
        }
    }

    public void select() throws WebException {
        select(new JSonDocTreeSerializer(ctx));
    }


    /**
    root=ID   - enter node ID
    toggle=ID - toggle expanded state for node ID
    */
    public void select(JSonDocTreeSerializer serializer) throws WebException {
        FormData form = ctx.getForm();
        String selection = form.getString("root");
        if (selection == null) {
            selection = form.getString("toggle");
            if (selection != null) {
                toggle(selection);
            }
        } else {
            enter(selection, serializer);
        }
    }

    public void enter(String path) throws WebException {
        enter(path, new JSonDocTreeSerializer(ctx));
    }

    public void enter(String path, JSonDocTreeSerializer serializer) throws WebException {
        loadTree();
        TreeItem item = tree.findAndReveal(path);
        item.expand();
        JSONArray result = new JSONArray();
        if (item != null && item.hasChildren()) {
            result = serializer.toJSON((item.getChildren()));
        }
        try {
System.out.println(result.toString());;
            ctx.print(result.toString());
        } catch (IOException e) {
            throw WebException.wrap(e);
        }
    }

    public void toggle(String path) throws WebException {
        loadTree();
        TreeItem item = tree.findAndReveal(path);
        if (item.isExpanded()) {
            item.collapse();
        } else {
            item.expand();
        }
    }
}
