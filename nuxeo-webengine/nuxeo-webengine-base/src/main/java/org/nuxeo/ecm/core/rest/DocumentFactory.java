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

package org.nuxeo.ecm.core.rest;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocumentFactory {

    // Utility class.
    private DocumentFactory() {
    }

    public static DocumentObject newDocumentRoot(WebContext ctx, String path) {
        return new DocumentRoot(ctx, path);
    }

    public static DocumentObject newDocumentRoot(WebContext ctx, DocumentRef ref) {
        return new DocumentRoot(ctx, ref);
    }

    public static DocumentObject newDocumentRoot(WebContext ctx, DocumentModel doc) {
        return new DocumentRoot(ctx, doc);
    }

    public static DocumentObject newDocument(WebContext ctx, String path) {
        PathRef pathRef = new PathRef(path);
        DocumentModel doc = ctx.getCoreSession().getDocument(pathRef);
        return (DocumentObject) ctx.newObject(doc.getType(), doc);
    }

    public static DocumentObject newDocument(WebContext ctx, DocumentRef ref) {
        DocumentModel doc = ctx.getCoreSession().getDocument(ref);
        return (DocumentObject) ctx.newObject(doc.getType(), doc);
    }

    public static DocumentObject newDocument(WebContext ctx, DocumentModel doc) {
        return (DocumentObject) ctx.newObject(doc.getType(), doc);
    }

}
