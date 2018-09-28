/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.List;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.restapi.jaxrs.io.RestConstants;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * This object basically overrides the default DocumentObject that doesn't know how to produce/consume JSON
 *
 * @since 5.7.2
 */

@WebObject(type = "Document")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity",
        MediaType.APPLICATION_JSON + "+esentity" })
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
     * @return the document or the last version document in case of versioning handled
     */
    @PUT
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public Response doPut(DocumentModel inputDoc, @Context HttpHeaders headers) {
        DocumentModelJsonReader.applyPropertyValues(inputDoc, doc);
        CoreSession session = ctx.getCoreSession();
        versioningDocFromHeaderIfExists(headers);
        updateCommentFromHeader(headers);
        try {
            doc = session.saveDocument(doc);
            session.save();
        } catch (ConcurrentUpdateException e) {
            return Response.status(Status.CONFLICT).entity("Invalid change token").build();
        }
        DocumentModel returnedDoc = isVersioning ? session.getLastDocumentVersion(doc.getRef()) : doc;
        return Response.ok(returnedDoc).build();
    }

    @POST
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public Response doPost(DocumentModel inputDoc) {
        CoreSession session = ctx.getCoreSession();

        if (StringUtils.isBlank(inputDoc.getType()) || StringUtils.isBlank(inputDoc.getName())) {
            return Response.status(Status.BAD_REQUEST).entity("type or name property is missing").build();
        }

        DocumentModel createdDoc = session.createDocumentModel(doc.getPathAsString(), inputDoc.getName(),
                inputDoc.getType());
        DocumentModelJsonReader.applyPropertyValues(inputDoc, createdDoc);
        createdDoc = session.createDocument(createdDoc);
        session.save();
        return Response.ok(createdDoc).status(Status.CREATED).build();
    }

    @DELETE
    public Response doDelete() {
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
        PathRef pathRef = new PathRef(doc.getPath().append(path).toString());
        DocumentModel doc = ctx.getCoreSession().getDocument(pathRef);
        return (DocumentObject) ctx.newObject("Document", doc);
    }

    @Override
    public DocumentObject newDocument(DocumentRef ref) {
        DocumentModel doc = ctx.getCoreSession().getDocument(ref);
        return (DocumentObject) ctx.newObject("Document", doc);
    }

    @Override
    public DocumentObject newDocument(DocumentModel doc) {
        return (DocumentObject) ctx.newObject("Document", doc);
    }

    /**
     * In case of version option header presence, checkin the related document
     *
     * @param headers X-Versioning-Option or Source (for automatic versioning) Header
     */
    private void versioningDocFromHeaderIfExists(HttpHeaders headers) {
        isVersioning = false;
        List<String> versionHeader = headers.getRequestHeader(RestConstants.X_VERSIONING_OPTION);
        List<String> sourceHeader = headers.getRequestHeader(RestConstants.SOURCE);
        if (versionHeader != null && !versionHeader.isEmpty()) {
            VersioningOption versioningOption = VersioningOption.valueOf(versionHeader.get(0).toUpperCase());
            if (versioningOption != null && !versioningOption.equals(VersioningOption.NONE)) {
                doc.putContextData(VersioningService.VERSIONING_OPTION, versioningOption);
                isVersioning = true;
            }
        } else if (sourceHeader != null && !sourceHeader.isEmpty()) {
            doc.putContextData(CoreSession.SOURCE, sourceHeader.get(0));
            isVersioning = true;
        }
    }

    /**
     * Fills the {@code doc} context data with a comment from the {@code Update-Comment} header if present.
     *
     * @since 9.3
     */
    protected void updateCommentFromHeader(HttpHeaders headers) {
        List<String> updateCommentHeader = headers.getRequestHeader(RestConstants.UPDATE_COMMENT_HEADER);
        if (updateCommentHeader != null && !updateCommentHeader.isEmpty()) {
            String comment = updateCommentHeader.get(0);
            doc.putContextData("comment", comment);
        }
    }
}
