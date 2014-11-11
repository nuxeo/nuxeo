/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.webengine.sites;

import net.sf.json.JSONArray;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.ui.tree.ContentProvider;
import org.nuxeo.ecm.webengine.ui.tree.JSonTreeSerializer;
import org.nuxeo.ecm.webengine.ui.tree.TreeItem;
import org.nuxeo.ecm.webengine.ui.tree.document.DocumentTree;

public class SiteDocumentTree extends DocumentTree {

    public SiteDocumentTree(WebContext ctx, DocumentModel rootDoc) {
        super(ctx, rootDoc);
    }

    @Override
    protected JSonTreeSerializer getSerializer(WebContext ctx) {
        return new SiteTreeSerializer();
    }

    @Override
    protected ContentProvider getProvider(WebContext ctx) {
        return new SiteContentProvider(ctx.getCoreSession());
    }

    @Override
    protected String enter(WebContext ctx, String path, JSonTreeSerializer serializer) {
        TreeItem item = tree.findAndReveal(path);
        if (item != null) {
            item.expand();
            if (!item.hasChildren()) {
                item.collapse();
            }
            JSONArray result = new JSONArray();
            if (item.isContainer()) {
                result = serializer.toJSON(item.getChildren());
            }
            return result.toString();
        } else {
            return null;
        }
    }

}
