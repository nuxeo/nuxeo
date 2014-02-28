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

import java.io.Serializable;

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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
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
@Produces({ "application/json+nxentity", MediaType.APPLICATION_JSON })
public class JSONDocumentObject extends DocumentObject {

    /**
     *
     */
    private static final String APPLICATION_JSON_NXENTITY = "application/json+nxentity";

    protected static final Log log = LogFactory.getLog(JSONDocumentObject.class);

    @Override
    @GET
    public DocumentModel doGet() {
        return doc;
    }

    @PUT
    @Consumes({ APPLICATION_JSON_NXENTITY, "application/json" })
    public DocumentModel doPut(DocumentModel inputDoc) throws ClientException {
        applyPropertyValues(inputDoc, doc);
        CoreSession session = ctx.getCoreSession();
        doc = session.saveDocument(doc);
        session.save();
        return doc;
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
        applyPropertyValues(inputDoc, createdDoc);
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
     * Decodes a Serializable to make it a blob.
     *
     * @since 5.9.1
     */
    private static Serializable decodeBlob(Serializable data) {
        if (data instanceof Blob) {
            return data;
        } else {
            return null;
        }
    }

    /**
     * Check that a serialized data is not null.
     *
     * @since 5.9.1
     */
    private static boolean isNotNull(Serializable data) {
        return data != null && !"null".equals(data);
    }

    private static void applyPropertyValues(DocumentModel src, DocumentModel dst)
            throws ClientException {
        for (String schema : src.getSchemas()) {
            DataModelImpl dataModel = (DataModelImpl) dst.getDataModel(schema);
            DataModel fromDataModel = src.getDataModel(schema);

            for (String field : fromDataModel.getDirtyFields()) {
                Serializable data = (Serializable) fromDataModel.getData(field);
                try {
                    if (isNotNull(data)) {
                        if (!(dataModel.getDocumentPart().get(field) instanceof BlobProperty)) {
                            dataModel.setData(field, data);
                        } else {
                            dataModel.setData(field, decodeBlob(data));
                        }
                    }
                } catch (PropertyNotFoundException e) {
                    log.warn(String.format(
                            "Trying to deserialize unexistent field : {%s}",
                            field));
                }
            }
        }
    }

}
