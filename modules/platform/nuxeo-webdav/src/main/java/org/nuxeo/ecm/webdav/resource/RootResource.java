/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.webdav.resource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jugs.webdav.jaxrs.methods.PROPFIND;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.webdav.backend.Backend;
import org.nuxeo.ecm.webdav.backend.BackendHelper;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.xml.sax.SAXException;

/**
 * Root JAX-RS resource for WebDAV. Path is set at the servlet level via deployment-fragment.xml.
 *
 * @since 2025.21
 */
@Path("")
public class RootResource {

    private static final Logger log = LogManager.getLogger(RootResource.class);

    private final HttpServletRequest request;

    public RootResource(@Context HttpServletRequest request, @Context WebContext webContext) {
        log.debug("{} {}", request::getMethod, request::getRequestURI);
        this.request = request;
        // Force proxy resolution so DefaultContext constructor stores itself as request attribute
        webContext.getRequest();
    }

    @GET
    @Produces("text/html")
    public Object getRoot() {
        var resource = findResource("");
        if (resource instanceof FolderResource folderResource) {
            return folderResource.get();
        }
        return ((VirtualFolderResource) resource).get();
    }

    @OPTIONS
    public Object getRootOptions() {
        var resource = findResource("");
        if (resource instanceof FolderResource folderResource) {
            return folderResource.options();
        }
        return ((VirtualFolderResource) resource).options();
    }

    @PROPFIND
    @Produces({ "application/xml", "text/xml" })
    public Object getRootPropfind(@Context UriInfo uriInfo, @HeaderParam("depth") String depth)
            throws IOException, JAXBException, URISyntaxException, ParserConfigurationException, SAXException {
        var resource = findResource("");
        if (resource instanceof FolderResource folderResource) {
            return folderResource.propfind(uriInfo, depth);
        }
        return ((VirtualFolderResource) resource).propfind(uriInfo, depth);
    }

    @Path("{path:.+}")
    public Object findResource(@PathParam("path") String path) {
        path = resolveRawPath();

        var backend = BackendHelper.getBackend(path, request);
        if (backend == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        if (backend.isVirtual()) {
            return new VirtualFolderResource(path, request, backend.getVirtualFolderNames());
        }

        var doc = resolveDocument(backend, path);
        if (doc == null) {
            return new UnknownResource(path, request, backend);
        }

        if (!backend.hasPermission(doc.getRef(), SecurityConstants.READ)) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }

        var docPath = getDocumentPath(doc);
        if (doc.isFolder()) {
            return new FolderResource(docPath, doc, request, backend);
        }
        return new FileResource(docPath, doc, request, backend);
    }

    private DocumentModel resolveDocument(Backend backend, String path) {
        try {
            return backend.getDocument(path);
        } catch (DocumentNotFoundException e) {
            log.error("Error during resolving path: {}", path, e);
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
    }

    /**
     * Resolves the actual request path from the raw request URI, preserving semicolons that Jersey/Tomcat would
     * otherwise strip as matrix parameter separators.
     */
    private String resolveRawPath() {
        var requestUri = request.getRequestURI();
        var prefix = request.getContextPath() + request.getServletPath() + "/";
        var rawPath = requestUri.substring(prefix.length());
        // Percent-decode using URI (not URLDecoder which treats '+' as space)
        return URI.create("/" + rawPath).getPath().substring(1);
    }

    private String getDocumentPath(DocumentModel source) {
        if (source.isFolder()) {
            return source.getPathAsString();
        }
        var bh = source.getAdapter(BlobHolder.class);
        if (bh != null) {
            var blob = bh.getBlob();
            if (blob != null) {
                return blob.getFilename();
            }
        }
        return String.valueOf(source.getPropertyValue("dc:title"));
    }

}
