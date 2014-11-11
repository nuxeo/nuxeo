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

package org.nuxeo.ecm.core.rest;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO: be able to use other repositories than the default
public class DocumentRoot extends DocumentObject {

    public DocumentRoot(WebContext ctx, String uri) {
        this(ctx, new PathRef(uri));
    }

    public DocumentRoot(WebContext ctx, DocumentRef root) {
        try {
            DocumentModel doc = ctx.getCoreSession().getDocument(root);
            initialize(ctx, ctx.getModule().getType(doc.getType()), doc);
            setRoot(true);
            ctx.push(this);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public DocumentRoot(WebContext ctx, DocumentModel root) {
        initialize(ctx, ctx.getModule().getType(root.getType()), root);
        setRoot(true);
        ctx.push(this);
    }

}
