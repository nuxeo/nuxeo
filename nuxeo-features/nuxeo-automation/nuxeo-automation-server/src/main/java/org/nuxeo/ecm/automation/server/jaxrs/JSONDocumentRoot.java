/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * Basically do the same thing than DocumentRoot but with JSONDocumentObject
 *
 * @since 5.7.2
 */
public class JSONDocumentRoot extends JSONDocumentObject{

    public JSONDocumentRoot(WebContext ctx, String uri) {
        this(ctx, new PathRef(uri));
    }

    public JSONDocumentRoot(WebContext ctx, DocumentRef root) {
        try {
            DocumentModel doc = ctx.getCoreSession().getDocument(root);
            initialize(ctx, ctx.getModule().getType("Document"), doc);
            setRoot(true);
            ctx.push(this);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public JSONDocumentRoot(WebContext ctx, DocumentModel root) {
        initialize(ctx, ctx.getModule().getType("Document"), root);
        setRoot(true);
        ctx.push(this);
    }


}
