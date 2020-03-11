/*
 * (C) Copyright 2013-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Damien Metzler
 *     Florent Guillaume
 */
package org.nuxeo.ecm.restapi.server.jaxrs.blob;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.versioning.VersioningService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.8
 */
@WebObject(type = "blob")
public class BlobObject extends DefaultObject {

    protected DocumentModel doc;

    protected DocumentBlobHolder blobHolder;

    // null if blob is not directly from a property
    protected String xpath;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        if (args.length != 2) {
            throw new IllegalArgumentException("BlobObject takes 2 parameters");
        }
        String path = (String) args[0];
        doc = (DocumentModel) args[1];
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (path == null) {
            // use default blob holder
            if (bh == null) {
                throw new WebResourceNotFoundException("No BlobHolder found");
            }
            if (!(bh instanceof DocumentBlobHolder)) {
                throw new WebResourceNotFoundException("Unknown BlobHolder class: " + bh.getClass().getName());
            }
            blobHolder = (DocumentBlobHolder) bh;
        } else if (path.startsWith(BLOBHOLDER_PREFIX)) {
            // use index in default blob holder
            // decoding logic from DownloadServiceImpl
            if (bh == null) {
                throw new WebResourceNotFoundException("No BlobHolder found");
            }
            if (!(bh instanceof DocumentBlobHolder)) {
                throw new WebResourceNotFoundException("Unknown BlobHolder class: " + bh.getClass().getName());
            }
            // suffix check
            String suffix = path.substring(BLOBHOLDER_PREFIX.length());
            int index;
            try {
                index = Integer.parseInt(suffix);
            } catch (NumberFormatException e) {
                throw new WebResourceNotFoundException("Invalid xpath: " + path);
            }
            if (!suffix.equals(Integer.toString(index))) {
                // attempt to use a non-canonical integer, could be used to bypass
                // a permission function checking just "blobholder:1" and receiving "blobholder:01"
                throw new WebResourceNotFoundException("Invalid xpath: " + path);
            }
            // find best BlobHolder to use
            if (index == 0) {
                blobHolder = (DocumentBlobHolder) bh;
            } else {
                blobHolder = ((DocumentBlobHolder) bh).asDirectBlobHolder(index);
            }
        } else {
            // use xpath
            // if the default adapted blob holder is the one with the same xpath, use it
            if (bh instanceof DocumentBlobHolder && ((DocumentBlobHolder) bh).getXpath().equals(path)) {
                blobHolder = (DocumentBlobHolder) bh;
            } else {
                // checking logic from DownloadServiceImpl
                if (!path.contains(":")) {
                    // attempt to use a xpath not prefix-qualified, could be used to bypass
                    // a permission function checking just "file:content" and receiving "content"
                    // try to add prefix
                    SchemaManager schemaManager = Framework.getService(SchemaManager.class);
                    // TODO precompute this in SchemaManagerImpl
                    int slash = path.indexOf('/');
                    String first = slash == -1 ? path : path.substring(0, slash);
                    for (Schema schema : schemaManager.getSchemas()) {
                        if (!schema.getNamespace().hasPrefix()) {
                            // schema without prefix, try it
                            if (schema.getField(first) != null) {
                                path = schema.getName() + ":" + path;
                                break;
                            }
                        }
                    }
                }
                if (!path.contains(":")) {
                    throw new WebResourceNotFoundException("Invalid xpath: " + path);
                }
                blobHolder = new DocumentBlobHolder(doc, path);
            }
        }
        xpath = blobHolder.getXpath();
    }

    @Override
    public <A> A getAdapter(Class<A> adapter) {
        if (Blob.class.isAssignableFrom(adapter)) {
            return adapter.cast(blobHolder.getBlob());
        }
        if (BlobHolder.class.isAssignableFrom(adapter)) {
            return adapter.cast(blobHolder);
        }
        return super.getAdapter(adapter);
    }

    public DocumentBlobHolder getBlobHolder() {
        return blobHolder;
    }

    @GET
    public Object doGet(@Context Request request) {
        if (blobHolder instanceof DocumentBlobHolder) {
            // managed by DocumentBlobHolderWriter
            return blobHolder;
        } else {
            // managed by BlobWriter
            Blob blob;
            try {
                blob = blobHolder.getBlob();
            } catch (PropertyNotFoundException e) {
                throw new WebResourceNotFoundException("Invalid xpath");
            }
            return blob;
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
            doc.getProperty(xpath).remove();
        } catch (PropertyNotFoundException e) {
            throw new NuxeoException("Failed to delete attached file into property: " + xpath, e, SC_BAD_REQUEST);
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
        try {
            doc.setPropertyValue(xpath, (Serializable) blob);
        } catch (PropertyNotFoundException e) {
            throw new NuxeoException("Failed to attach file into property: " + xpath, e, SC_BAD_REQUEST);
        }
        // make snapshot
        doc.putContextData(VersioningService.VERSIONING_OPTION, form.getVersioningOption());
        CoreSession session = ctx.getCoreSession();
        session.saveDocument(doc);
        session.save();
        return Response.ok("blob updated").build();
    }

}
