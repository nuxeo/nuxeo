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

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * Resolves a document URL given its ID
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li> GET - get the document index view
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name="nxdoc", type="DocumentResolverService")
public class DocumentResolverService extends DefaultAdapter {

    @Path("{id}")
    public DocumentObject doGet(@PathParam("id") String id) {
        try {
            DocumentModel doc = ctx.getCoreSession().getDocument(new IdRef(id));
            return DocumentFactory.newDocument(ctx, doc);
        } catch (Exception e) {
            throw WebException.wrap("Failed to get lock on document", e);
        }
    }

}
