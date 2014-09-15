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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JSONDocumentModelReader;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.restapi.jaxrs.io.RestConstants;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;

import java.util.List;

/**
 * This object basically overrides the default DocumentObject that doesn't know
 * how to produce/consume JSON
 *
 * @since 5.7.2
 */

@WebObject(type = "Document")
@Produces({ "application/json+nxentity", "application/json+esentity", MediaType.APPLICATION_JSON })
public class JSONDocumentObject extends DocumentObject {

    private static final String APPLICATION_JSON_NXENTITY = "application/json+nxentity";

    protected static final Log log = LogFactory.getLog(JSONDocumentObject.class);

    private boolean isVersioning;

    @Override
    @GET
    public DocumentModel doGet() {
        return doc;
    }

    /**
     * @return the document or the last version document in case of
     * versioning handled
     */
    @PUT
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public DocumentModel doPut(DocumentModel inputDoc,
            @Context HttpHeaders headers) throws ClientException {
        JSONDocumentModelReader.applyPropertyValues(inputDoc, doc);
        CoreSession session = ctx.getCoreSession();
        versioningDocFromHeaderIfExists(headers);
        doc = session.saveDocument(doc);
        session.save();
        return isVersioning ? session.getLastDocumentVersion(doc.getRef()) :
                doc;
    }

    @POST
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public Response doPost(DocumentModel inputDoc) throws ClientException {
        CoreSession session = ctx.getCoreSession();

        if (StringUtils.isBlank(inputDoc.getType())
                || StringUtils.isBlank(inputDoc.getName())) {
            return Response.status(Status.BAD_REQUEST).entity(
                    "type or name property is missing").build();
        }

        DocumentModel createdDoc = session.createDocumentModel(
                doc.getPathAsString(), inputDoc.getName(), inputDoc.getType());
        JSONDocumentModelReader.applyPropertyValues(inputDoc, createdDoc);
        createdDoc = session.createDocument(createdDoc);
        session.save();
        return Response.ok(createdDoc).status(Status.CREATED).build();
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

    /**
     * In case of version option header presence, checkin the related document
     *
     * @param headers X-Versioning-Option Header
     */
    private void versioningDocFromHeaderIfExists(HttpHeaders headers) throws
            ClientException {
        isVersioning = false;
        List<String> versionHeader = headers.getRequestHeader
                (RestConstants.X_VERSIONING_OPTION);
        if (versionHeader != null && versionHeader.size() != 0) {
            VersioningOption versioningOption = VersioningOption.valueOf
                    (versionHeader.get(0).toUpperCase());
            if (versioningOption != null && !versioningOption.equals
                    (VersioningOption.NONE)) {
                doc.putContextData(VersioningService.VERSIONING_OPTION,
                        versioningOption);
                isVersioning = true;
            }
        }
    }
}
