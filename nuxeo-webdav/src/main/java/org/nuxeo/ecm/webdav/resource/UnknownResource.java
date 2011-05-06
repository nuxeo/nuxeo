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

import net.java.dev.webdav.jaxrs.methods.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.webdav.Util;
import org.nuxeo.ecm.webdav.backend.WebDavBackend;
import org.nuxeo.runtime.services.streaming.InputStreamSource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * Resource for an unknown (ie non-existing) object.
 * Used so that PUT / MKCOL requests can actually created a document / folder.
 * Other requests will end up with a 404 error.
 */
public class UnknownResource extends AbstractResource {

    private static final Log log = LogFactory.getLog(UnknownResource.class);

    protected WebDavBackend backend;

    public UnknownResource(String path, HttpServletRequest request, WebDavBackend backend) throws Exception {
        super(path, request);
        this.backend = backend;
    }

    /**
     * PUT over a non-existing resource: create a new file resource.
     */
    @PUT
    public Response put() throws Exception {

        // Special case: ignore magic MacOS files.
        // Actually, we shouldn't do this. This breaks drag'n'drop of files downloaded from the
        // Internet (Finder fails with error "Unable to quarantine" in the logs).
        // TODO: find a better way.
        /*
        if (name.startsWith("._")) {
            Util.endTransaction();
            // Not sure if it's the right error code.
            throw new WebApplicationException(409);
        }
        */

        ensureParentExists();
        Blob content = new StreamingBlob(new InputStreamSource(request.getInputStream()));
        // note that content.getLentgh from an input stream source always return -1
        if (request.getContentLength() > 0) {
            String contentType = request.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            content.setMimeType(contentType);
            content.setFilename(name);
            backend.createFile(parentPath, name, content);
        } else {
            backend.createFile(parentPath, name);
        }

        backend.saveChanges();
        return Response.created(new URI(request.getRequestURI())).build();
    }

    /**
     * MKCOL over a non-existing resource: create a new folder resource.
     */
    @MKCOL
    public Response mkcol() throws Exception {
        ensureParentExists();

        //We really need this?
        InputStreamSource iss = new InputStreamSource(request.getInputStream());
        if (iss.getString().length() > 0) {
            return Response.status(415).build();
        }

        backend.createFolder(parentPath, name);
        backend.saveChanges();
        return Response.created(new URI(request.getRequestURI())).build();
    }

    // All these methods should raise an error 404

    @DELETE
    public Response delete() {
        return Response.status(404).build();
    }

    @COPY
    public Response copy() {
        return Response.status(404).build();
    }

    @MOVE
    public Response move() {
        return Response.status(404).build();
    }

    @PROPFIND
    public Response propfind() {
        return Response.status(404).build();
    }

    @PROPPATCH
    public Response proppatch() {
        return Response.status(404).build();
    }

    @HEAD
    public Response head() {
        return Response.status(404).build();
    }

    // Utility

    private void ensureParentExists() throws Exception {
        if (!backend.exists(parentPath)) {
            throw new WebApplicationException(409);
        }
    }

}
