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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;

import net.java.dev.webdav.jaxrs.methods.PROPFIND;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.backend.BackendHelper;

//path is set at the servlet level, see the deployment-fragment file
@Path("")
public class RootResource {

    private static final Log log = LogFactory.getLog(RootResource.class);

    private HttpServletRequest request;

    public RootResource(@Context HttpServletRequest request) {
        log.debug(request.getMethod() + " " + request.getRequestURI());
        this.request = request;
    }

    @GET
    @Produces("text/html")
    public Object getRoot() {
        Object resource = findResource("");
        if (resource instanceof FolderResource) {
            return ((FolderResource) resource).get();
        } else {
            return ((VirtualFolderResource) findResource("")).get();
        }
    }

    @OPTIONS
    public Object getRootOptions() {
        Object resource = findResource("");
        if (resource instanceof FolderResource) {
            return ((FolderResource) resource).options();
        } else {
            return ((VirtualFolderResource) findResource("")).options();
        }
    }

    @PROPFIND
    @Produces({ "application/xml", "text/xml" })
    public Object getRootPropfind(@Context UriInfo uriInfo, @HeaderParam("depth") String depth)
            throws IOException, JAXBException, URISyntaxException {
        Object resource = findResource("");
        if (resource instanceof FolderResource) {
            return ((FolderResource) resource).propfind(uriInfo, depth);
        } else {
            return ((VirtualFolderResource) findResource("")).propfind(uriInfo, depth);
        }
    }

    @Path("{path:.+}")
    public Object findResource(@PathParam("path") String path) {
        try {
            path = new String(path.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException(e);
        }

        Backend backend = BackendHelper.getBackend(path, request);

        if (backend == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        if (backend.isVirtual()) {
            return new VirtualFolderResource(path, request, backend.getVirtualFolderNames());
        }

        DocumentModel doc = null;
        try {
            doc = backend.getDocument(path);
        } catch (DocumentNotFoundException e) {
            log.error("Error during resolving path: " + path, e);
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
            return new FolderResource(getDocumentPath(doc), doc, request, backend);
        } else {
            return new FileResource(getDocumentPath(doc), doc, request, backend);
        }
    }

    private String getDocumentPath(DocumentModel source) {
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
