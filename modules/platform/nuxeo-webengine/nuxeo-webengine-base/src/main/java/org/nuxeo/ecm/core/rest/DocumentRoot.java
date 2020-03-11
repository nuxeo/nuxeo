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
// TODO: be able to use other repositories than the default
public class DocumentRoot extends DocumentObject {

    public DocumentRoot(WebContext ctx, String uri) {
        this(ctx, new PathRef(uri));
    }

    public DocumentRoot(WebContext ctx, DocumentRef root) {
        DocumentModel doc = ctx.getCoreSession().getDocument(root);
        initialize(ctx, ctx.getModule().getType(doc.getType()), doc);
        setRoot(true);
        ctx.push(this);
    }

    public DocumentRoot(WebContext ctx, DocumentModel root) {
        initialize(ctx, ctx.getModule().getType(root.getType()), root);
        setRoot(true);
        ctx.push(this);
    }

}
