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

import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.session.AbstractComponent;
import org.nuxeo.ecm.webengine.session.SessionException;
import org.nuxeo.ecm.webengine.session.UserSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class JSonTree extends AbstractComponent {

    private static final long serialVersionUID = 1L;

    protected TreeViewImpl tree;


    public JSonTree() {
    }

    public TreeView getTree() {
        return tree;
    }


    protected abstract Object getInput(WebContext ctx) throws WebException;
    protected abstract ContentProvider  getProvider(WebContext ctx) throws WebException;
    protected abstract JSonTreeSerializer getSerializer(WebContext ctx) throws WebException;


    @Override
    public void doInitialize(UserSession session, String name)
            throws SessionException {
        try {
            tree = new TreeViewImpl();
        } catch (Exception e) {
            throw new SessionException("Failed to initialize tree component "+getClass()+"#"+name+ ". Cause: "+e.getMessage(), e);
        }
    }

    @Override
    public void doDestroy(UserSession session) throws SessionException {
        tree = null;
    }

    public void updateSelection(WebContext ctx) throws WebException {
        updateSelection(ctx, getProvider(ctx), getSerializer(ctx));
    }

    /**
    root=ID   - enter node ID
    toggle=ID - toggle expanded state for node ID
    */
    public synchronized void updateSelection(WebContext ctx, ContentProvider provider, JSonTreeSerializer serializer) throws WebException {
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
                String result = enter(ctx, selection, serializer);
                if (result != null) {
                    try {
                      System.out.println(result);
                        ctx.print(result);
                    } catch (IOException e) {
                        throw WebException.wrap(e);
                    }
                } else {
                    ctx.getLog().warn("TreeItem: "+selection+" not found");
                }
            }
        } finally {
            tree.setContentProvider(null);
        }
    }

    protected String enter(WebContext ctx, String path, JSonTreeSerializer serializer) throws WebException {
        TreeItem item = tree.findAndReveal(path);
        if (item != null) {
            item.expand();
            JSONArray result = new JSONArray();
            if (item != null && item.isContainer()) {
                result = serializer.toJSON((item.getChildren()));
            }
            return result.toString();
        } else {
            return null;
        }
    }

    protected void toggle(String path) throws WebException {
        TreeItem item = tree.findAndReveal(path);
        if (item.isExpanded()) {
            item.collapse();
        } else {
            item.expand();
        }
    }
}
