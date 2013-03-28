/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webdav.resource;

import net.java.dev.webdav.core.jaxrs.xml.properties.IsFolder;
import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.CreationDate;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentLength;
import net.java.dev.webdav.jaxrs.xml.properties.GetContentType;
import net.java.dev.webdav.jaxrs.xml.properties.GetLastModified;
import net.java.dev.webdav.jaxrs.xml.properties.LockDiscovery;
import net.java.dev.webdav.jaxrs.xml.properties.SupportedLock;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.webdav.Util;
import org.nuxeo.ecm.webdav.backend.WebDavBackend;
import org.nuxeo.runtime.services.streaming.InputStreamSource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

import static javax.ws.rs.core.Response.Status.OK;

/**
 * Resource representing a file-like object in the repository. (I.e. not a folder).
 */
public class FileResource extends ExistingResource {

    private static final Log log = LogFactory.getLog(FileResource.class);

    public FileResource(String path, DocumentModel doc, HttpServletRequest request, WebDavBackend backend) throws Exception {
        super(path, doc, request, backend);
    }

    @GET
    public Object get() throws Exception {
        Blob content = null;
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            try {
                content = bh.getBlob();
            } catch (ClientException e) {
                log.error("Unable to get blob", e);
            }
        }
        if (content == null) {
            return Response.ok("").build();
        } else {
            String mimeType;
            try {
                mimeType = content.getMimeType();
            } catch (Exception e) {
                mimeType = "application/octet-stream";
            }
            if ("???".equals(mimeType)) {
                mimeType = "application/octet-stream";
            }
            return Response.ok(content).type(mimeType).build();
        }
    }

    @PUT
    public Response put() throws Exception {
        if (backend.isLocked(doc.getRef()) && !backend.canUnlock(doc.getRef())) {
            return Response.status(423).build();
        }

        try {
            Blob content = new StreamingBlob(new InputStreamSource(
                    request.getInputStream()));
            content.persist();
            String contentType = request.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            content.setMimeType(contentType);
            content.setFilename(name);

            backend.updateDocument(doc, name, content);
        return Response.created(new URI(URLEncoder.encode(path, "UTF8"))).build();
        } catch (Exception e) {
            log.error("Error during PUT method execution", e);
            return Response.status(409).build();
        }
    }

    @PROPFIND
    public Response propfind(@Context UriInfo uriInfo) throws Exception {

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
        //Util.printAsXml(prop);

        PropStatBuilderExt props = getPropStatBuilderExt(doc, uriInfo);
        PropStat propStatFound = props.build();
        PropStat propStatNotFound = null;
        if (prop != null) {
            propStatNotFound = props.notFound(prop);
        }

        net.java.dev.webdav.jaxrs.xml.elements.Response response;
        URI uri = uriInfo.getRequestUri();
        PropStat filePropStat = new PropStat(
                new Prop(new SupportedLock(new LockEntry(LockScope.EXCLUSIVE, LockType.WRITE))), 
                new Status(OK));
        if (doc.isLocked()) {
            PropStat lockDiscoveryPropStat = new PropStat(
                    new Prop(getLockDiscovery(doc, uriInfo)), new Status(OK));
            if (propStatNotFound != null) {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uri), null, null, null, 
                        filePropStat, propStatFound, propStatNotFound, lockDiscoveryPropStat);
            } else {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uri), null, null, null, 
                        filePropStat, propStatFound, lockDiscoveryPropStat);
            }
        } else {
            if (propStatNotFound != null) {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uri), null, null, null, 
                        filePropStat, propStatFound, propStatNotFound);
            } else {
                response = new net.java.dev.webdav.jaxrs.xml.elements.Response(
                        new HRef(uri), null, null, null, 
                        filePropStat, propStatFound);
            }
        }
        
        MultiStatus st = new MultiStatus(response);
        return Response.status(207).entity(st).build();
    }

}
