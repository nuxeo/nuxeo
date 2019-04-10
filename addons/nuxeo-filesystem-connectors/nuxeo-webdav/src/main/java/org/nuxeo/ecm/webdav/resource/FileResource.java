/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webdav.resource;

import static javax.ws.rs.core.Response.Status.OK;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.xml.elements.HRef;
import net.java.dev.webdav.jaxrs.xml.elements.LockEntry;
import net.java.dev.webdav.jaxrs.xml.elements.LockScope;
import net.java.dev.webdav.jaxrs.xml.elements.LockType;
import net.java.dev.webdav.jaxrs.xml.elements.MultiStatus;
import net.java.dev.webdav.jaxrs.xml.elements.Prop;
import net.java.dev.webdav.jaxrs.xml.elements.PropFind;
import net.java.dev.webdav.jaxrs.xml.elements.PropStat;
import net.java.dev.webdav.jaxrs.xml.elements.Status;
import net.java.dev.webdav.jaxrs.xml.properties.SupportedLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.jaxrs.Util;

/**
 * Resource representing a file-like object in the repository. (I.e. not a folder).
 */
public class FileResource extends ExistingResource {

    private static final Log log = LogFactory.getLog(FileResource.class);

    public FileResource(String path, DocumentModel doc, HttpServletRequest request, Backend backend) {
        super(path, doc, request, backend);
    }

    @GET
    public Object get() {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        Blob blob = bh == null ? null : bh.getBlob();
        if (blob == null) {
            return Response.ok("").build();
        }
        String mimeType = blob.getMimeType();
        if (mimeType.equals("???")) {
            mimeType = "application/octet-stream";
        }
        // provide full DocumentBlobHolder context if possible, to give more info when calling the DownloadService
        Object entity = bh instanceof DocumentBlobHolder ? bh : blob;
        return Response.ok(entity).type(mimeType).build();
    }

    @PUT
    public Response put() {
        if (backend.isLocked(doc.getRef()) && !backend.canUnlock(doc.getRef())) {
            return Response.status(423).build();
        }

        try {
            Blob content = Blobs.createBlob(request.getInputStream());
            String contentType = request.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            content.setMimeType(contentType);
            content.setFilename(name);

            backend.updateDocument(doc, name, content);
            try {
                return Response.created(new URI(URLEncoder.encode(path, "UTF8"))).build();
            } catch (URISyntaxException e) {
                throw new NuxeoException(e);
            }
        } catch (IOException e) {
            log.error("Error during PUT method execution", e);
            return Response.status(409).build();
        }
    }

    @PROPFIND
    @Produces({ "application/xml", "text/xml" })
    public Response propfind(@Context UriInfo uriInfo) throws IOException, JAXBException {

        Unmarshaller u = Util.getUnmarshaller();

        Prop prop = null;
        if (request.getInputStream() != null && request.getContentLength() > 0) {
            PropFind propFind;
            try {
                propFind = (PropFind) u.unmarshal(request.getInputStream());
            } catch (JAXBException e) {
                return Response.status(400).build();
            }
            prop = propFind.getProp();
        }
        // Util.printAsXml(prop);

        PropStatBuilderExt props = getPropStatBuilderExt(doc, uriInfo);
        PropStat propStatFound = props.build();
        PropStat propStatNotFound = null;
        if (prop != null) {
            propStatNotFound = props.notFound(prop);
        }

        net.java.dev.webdav.jaxrs.xml.elements.Response response;
        URI uri = uriInfo.getRequestUri();
        PropStat filePropStat = new PropStat(new Prop(new SupportedLock(new LockEntry(LockScope.EXCLUSIVE,
                LockType.WRITE))), new Status(OK));
        if (doc.isLocked()) {
            PropStat lockDiscoveryPropStat = new PropStat(new Prop(getLockDiscovery(doc, uriInfo)), new Status(OK));
            if (propStatNotFound != null) {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        filePropStat, propStatFound, propStatNotFound, lockDiscoveryPropStat);
            } else {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        filePropStat, propStatFound, lockDiscoveryPropStat);
            }
        } else {
            if (propStatNotFound != null) {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        filePropStat, propStatFound, propStatNotFound);
            } else {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(new HRef(uri), null, null, null,
                        filePropStat, propStatFound);
            }
        }

        MultiStatus st = new MultiStatus(response);
        return Response.status(207).entity(st).build();
    }

}
