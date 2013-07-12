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
package org.nuxeo.ecm.automation.rest.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 *
 *
 * @since TODO
 */

@WebObject(type = "Document")
@Produces(MediaType.APPLICATION_JSON)
public class JSONDocumentObject extends DocumentObject {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Override
    public DocumentModel doGet() {
        return doc;
    }


    @Override
    public DocumentObject newDocument(String path) {
        try {
            PathRef pathRef = new PathRef(doc.getPath().append(path).toString());
            DocumentModel doc = ctx.getCoreSession().getDocument(pathRef);
            return (DocumentObject) ctx.newObject("Document", doc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @Override
    public DocumentObject newDocument(DocumentRef ref) {
        try {
            DocumentModel doc = ctx.getCoreSession().getDocument(ref);
            return (DocumentObject) ctx.newObject("Document", doc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @Override
    public DocumentObject newDocument(DocumentModel doc) {
        try {
            return (DocumentObject) ctx.newObject("Document", doc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }
}
