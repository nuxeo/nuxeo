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

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * Resolves a document URL given its ID
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li>GET - get the document index view
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name = "nxdoc", type = "DocumentResolverService")
public class DocumentResolverService extends DefaultAdapter {

    @Path("{id}")
    public DocumentObject doGet(@PathParam("id") String id) {
        try {
            DocumentModel doc = ctx.getCoreSession().getDocument(new IdRef(id));
            return DocumentFactory.newDocument(ctx, doc);
        } catch (NuxeoException e) {
            e.addInfo("Failed to get lock on document");
            throw e;
        }
    }

}
