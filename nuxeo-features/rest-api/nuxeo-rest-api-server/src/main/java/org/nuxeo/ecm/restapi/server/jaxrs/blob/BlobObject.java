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
package org.nuxeo.ecm.restapi.server.jaxrs.blob;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * @since 5.8
 */
@WebObject(type = "blob")
public class BlobObject extends DefaultObject {

    public static final String BLOB_HOLDER_PREFIX = "blobholder:";

    private String fieldPath;

    private DocumentModel doc;

    private BlobHolder bh;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        if (args.length == 2) {
            fieldPath = (String) args[0];
            doc = (DocumentModel) args[1];

            if (fieldPath == null) {
                if (bh == null && doc.hasSchema("file")) {
                    fieldPath = "file:content";
                } else {
                    throw new IllegalArgumentException("No xpath specified and document does not have 'file' schema");
                }
            } else {
                if (fieldPath.startsWith(BLOB_HOLDER_PREFIX)) {
                    bh = doc.getAdapter(BlobHolder.class);
                    if (bh != null) {
                        fieldPath = fieldPath.replace(BLOB_HOLDER_PREFIX, "");
                    } else {
                        throw new WebResourceNotFoundException("No BlobHolder found");
                    }
                }
            }
        }
    }

    @Override
    public <A> A getAdapter(Class<A> adapter) {
        if (adapter.isAssignableFrom(Blob.class)) {
            try {
                return adapter.cast(blob(fieldPath));
            } catch (ClientException e) {
                throw WebException.wrap("Could not find any blob: " + fieldPath, e);
            }
        }
        return super.getAdapter(adapter);
    }

    protected Blob blob(String xpath) throws ClientException {
        if (bh != null) {
            if (StringUtils.isBlank(fieldPath) || fieldPath.equals("0")) {
                return bh.getBlob();
            } else {
                int index = Integer.parseInt(fieldPath);
                return bh.getBlobs().get(index);
            }
        }
        return (Blob) doc.getPropertyValue(xpath);
    }

    @GET
    public Object doGet(@Context Request request) throws ClientException {
        try {
            Blob blob = blob(fieldPath);
            if (blob == null) {
                throw new WebResourceNotFoundException("No attached file at " + fieldPath);
            }
            return blob;
        } catch (ClientException e) {
            throw WebException.wrap("Failed to get the attached file", e);
        }
    }

    /**
     * @deprecated since 7.3. Now returns directly the Blob and use default {@code BlobWriter}.
     */
    @Deprecated
    public static Response buildResponseFromBlob(Request request, HttpServletRequest httpServletRequest, Blob blob,
            String filename) {
        if (filename == null) {
            filename = blob.getFilename();
        }

        String digest = blob.getDigest();
        EntityTag etag = digest == null ? null : new EntityTag(digest);
        if (etag != null) {
            Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
            if (builder != null) {
                return builder.build();
            }
        }
        String contentDisposition = ServletHelper.getRFC2231ContentDisposition(httpServletRequest, filename);
        // cached resource did change or no ETag -> serve updated content
        Response.ResponseBuilder builder = Response.ok(blob).header("Content-Disposition", contentDisposition).type(
                blob.getMimeType());
        if (etag != null) {
            builder.tag(etag);
        }
        return builder.build();
    }

    @DELETE
    public Response doDelete() {
        try {
            if (bh != null) {
                throw new IllegalArgumentException("Cannot modify a Blob using a BlobHolder");
            }
            doc.getProperty(fieldPath).remove();
            CoreSession session = ctx.getCoreSession();
            session.saveDocument(doc);
            session.save();
        } catch (ClientException e) {
            throw WebException.wrap("Failed to delete attached file into property: " + fieldPath, e);
        }
        return Response.noContent().build();
    }

    @PUT
    public Response doPut() {
        FormData form = ctx.getForm();
        Blob blob = form.getFirstBlob();
        if (blob == null) {
            throw new IllegalArgumentException("Could not find any uploaded file");
        }

        try {
            if (bh != null) {
                throw new IllegalArgumentException("Cannot modify a Blob using a BlobHolder");
            }

            doc.setPropertyValue(fieldPath, (Serializable) blob);
            // make snapshot
            doc.putContextData(VersioningService.VERSIONING_OPTION, form.getVersioningOption());
            CoreSession session = ctx.getCoreSession();
            session.saveDocument(doc);
            session.save();
            return Response.ok("blob updated").build();
        } catch (ClientException e) {
            throw WebException.wrap("Failed to attach file", e);
        }
    }

}
