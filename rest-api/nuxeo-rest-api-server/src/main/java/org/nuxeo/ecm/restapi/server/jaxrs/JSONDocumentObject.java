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
package org.nuxeo.ecm.restapi.server.jaxrs;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * This object basically overrides the default DocumentObject that doesn't know
 * how to produce/consume JSON
 *
 * @since 5.7.2
 */

@WebObject(type = "Document")
@Produces({ "application/json+nxentity", "application/json+esentity", MediaType.APPLICATION_JSON })
public class JSONDocumentObject extends DocumentObject {

    /**
     *
     */
    private static final String APPLICATION_JSON_NXENTITY = "application/json+nxentity";

    @Override
    @GET
    public DocumentModel doGet() {
        return doc;
    }

    @PUT
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public DocumentModel doPut(DocumentModel doc) throws ClientException {
        CoreSession session = ctx.getCoreSession();
        doc = session.saveDocument(doc);
        session.save();
        return doc;
    }

    @POST
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public Response doPost(DocumentModel inputDoc) throws ClientException {
        CoreSession session = ctx.getCoreSession();

        inputDoc.setPathInfo(doc.getPathAsString(), inputDoc.getPathAsString());
        inputDoc = session.createDocument(inputDoc);
        session.save();
        return Response.ok(inputDoc).status(Status.CREATED).build();
    }

    @DELETE
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public Response doDeleteJson() throws ClientException {
        super.doDelete();
        return Response.noContent().build();
    }

    @Override
    @Path("@search")
    public Object search() {
        return ctx.newAdapter(this, "search");
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
