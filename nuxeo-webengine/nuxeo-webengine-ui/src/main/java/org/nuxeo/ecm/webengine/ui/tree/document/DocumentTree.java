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

package org.nuxeo.ecm.webengine.ui.tree.document;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.ContentProvider;
import org.nuxeo.ecm.webengine.ui.tree.JSonTree;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentTree extends JSonTree {

    private static final long serialVersionUID = 1L;

    public static void updateSelection(WebContext ctx, String treeId) throws Exception {
        ctx.getUserSession().getComponent(DocumentTree.class, treeId).updateSelection(ctx);
    }

    public static void updateSelection(WebContext ctx, String treeId,
            ContentProvider provider, JSonTreeSerializer serializer) throws Exception {
        ctx.getUserSession().getComponent(DocumentTree.class, treeId).updateSelection(ctx, provider, serializer);
    }

    @Override
    public void updateSelection(WebContext ctx) {
        super.updateSelection(ctx, getProvider(ctx), getSerializer(ctx));
    }

    @Override
    protected ContentProvider getProvider(WebContext ctx) {
        return new DocumentContentProvider(ctx.getCoreSession());
    }

    @Override
    protected JSonTreeSerializer getSerializer(WebContext ctx) {
        return new JSonDocumentTreeSerializer(ctx);
    }

    @Override
    protected Object getInput(WebContext ctx) {
        try {
            return ctx.getCoreSession().getRootDocument();
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

}
