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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.WebApplicationException;

import net.java.dev.webdav.jaxrs.methods.PROPFIND;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.backend.WebDavBackend;

import com.sun.jersey.spi.CloseableService;

@Path("dav")
public class RootResource {

    /**
     * Global root path computed once to serve as a prefix to URLs when needed.
     */
    static String rootPath;

    private static final Log log = LogFactory.getLog(RootResource.class);

    private HttpServletRequest request;

    public RootResource(@Context HttpServletRequest request,
            @Context CloseableService closeableService) throws Exception {
        log.debug(request.getMethod() + " " + request.getRequestURI());

        this.request = request;

        if (rootPath == null) {
            rootPath = request.getContextPath() + request.getServletPath();
            log.info(rootPath);
        }

    }

    @GET
    @Produces("text/html")
    public Object getRoot() throws Exception {
        Object resource = findResource("");
        if (resource instanceof FolderResource) {
            return ((FolderResource) resource).get();
        } else {
            return ((VirtualFolderResource) findResource("")).get();
        }
    }

    @OPTIONS
    public Object getRootOptions() throws Exception {
        Object resource = findResource("");
        if (resource instanceof FolderResource) {
            return ((FolderResource) resource).options();
        } else {
            return ((VirtualFolderResource) findResource("")).options();
        }
    }

    @PROPFIND
    public Object getRootPropfind(@Context UriInfo uriInfo,
            @HeaderParam("depth") String depth) throws Exception {
        Object resource = findResource("");
        if (resource instanceof FolderResource) {
            return ((FolderResource) resource).propfind(uriInfo, depth);
        } else {
            return ((VirtualFolderResource) findResource("")).propfind(uriInfo,
                    depth);
        }
    }

    @Path("{path:.+}")
    public Object findResource(@PathParam("path") String path) throws Exception {
        path = new String(path.getBytes(), "UTF-8");

        WebDavBackend backend = Backend.get(path, request);

        if (backend == null) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }

        if (backend.isVirtual()) {
            return new VirtualFolderResource(path, request,
                    backend.getVirtualFolderNames());
        }

        DocumentModel doc = null;
        try {
            doc = backend.resolveLocation(path);
        } catch (Exception e) {
            log.error("Error during resolving path: " + path + " Message:"
                    + e.getMessage());
            throw new WebApplicationException(Response.Status.CONFLICT);
        }

        if (doc == null) {
            return new UnknownResource(path, request, backend);
        }

        // Send 401 error if not authorised to read.
        if (!backend.hasPermission(doc.getRef(), SecurityConstants.READ)) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }

        if (doc.isFolder()) {
            return new FolderResource(getDocumentPath(doc), doc, request,
                    backend);
        } else {
            return new FileResource(getDocumentPath(doc), doc, request, backend);
        }
    }

    private String getDocumentPath(DocumentModel source) throws ClientException {
        if (source.isFolder()) {
            return source.getPathAsString();
        } else {
            BlobHolder bh = source.getAdapter(BlobHolder.class);
            if (bh != null) {
                Blob blob = bh.getBlob();
                if (blob != null) {
                    return blob.getFilename();
                }
            }
            return String.valueOf(source.getPropertyValue("dc:title"));
        }
    }

}
