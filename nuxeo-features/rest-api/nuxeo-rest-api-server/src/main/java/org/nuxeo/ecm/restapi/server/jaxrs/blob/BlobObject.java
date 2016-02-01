/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.restapi.server.jaxrs.blob;

import static org.nuxeo.ecm.core.io.download.DownloadService.BLOBHOLDER_PREFIX;

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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
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
                if (fieldPath.startsWith(BLOBHOLDER_PREFIX)) {
                    bh = doc.getAdapter(BlobHolder.class);
                    if (bh != null) {
                        fieldPath = fieldPath.replace(BLOBHOLDER_PREFIX, "");
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
            return adapter.cast(getBlob());
        }
        return super.getAdapter(adapter);
    }

    protected Blob getBlob() {
        if (bh != null) {
            if (StringUtils.isBlank(fieldPath) || fieldPath.equals("0")) {
                return bh.getBlob();
            } else {
                int index = Integer.parseInt(fieldPath);
                return bh.getBlobs().get(index);
            }
        }
        return (Blob) doc.getPropertyValue(fieldPath);
    }

    public BlobHolder getBlobHolder() {
        return bh;
    }

    /**
     * @since 8.2
     */
    public DocumentModel getDocument() {
        return doc;
    }

    /**
     * @since 8.2
     */
    public String getXpath() {
        return fieldPath;
    }

    @GET
    public Object doGet(@Context Request request) {
        Blob blob = getBlob();
        if (blob == null) {
            throw new WebResourceNotFoundException("No attached file at " + fieldPath);
        }
        return blob;
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
        if (bh != null) {
            throw new IllegalArgumentException("Cannot modify a Blob using a BlobHolder");
        }
        try {
            doc.getProperty(fieldPath).remove();
        } catch (PropertyException e) {
            throw WebException.wrap("Failed to delete attached file into property: " + fieldPath, e);
        }
        CoreSession session = ctx.getCoreSession();
        session.saveDocument(doc);
            session.save();
        return Response.noContent().build();
    }

    @PUT
    public Response doPut() {
        FormData form = ctx.getForm();
        Blob blob = form.getFirstBlob();
        if (blob == null) {
            throw new IllegalArgumentException("Could not find any uploaded file");
        }
        if (bh != null) {
            throw new IllegalArgumentException("Cannot modify a Blob using a BlobHolder");
        }
        try {
            doc.setPropertyValue(fieldPath, (Serializable) blob);
        } catch (PropertyException e) {
            throw WebException.wrap("Failed to attach file", e);
        }
        // make snapshot
        doc.putContextData(VersioningService.VERSIONING_OPTION, form.getVersioningOption());
        CoreSession session = ctx.getCoreSession();
        session.saveDocument(doc);
        session.save();
        return Response.ok("blob updated").build();
    }

}
