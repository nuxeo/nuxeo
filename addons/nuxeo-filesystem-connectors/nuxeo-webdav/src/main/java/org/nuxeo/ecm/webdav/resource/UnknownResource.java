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
 *     Stefane Fermigier
 */
package org.nuxeo.ecm.webdav.resource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import net.java.dev.webdav.jaxrs.methods.COPY;
import net.java.dev.webdav.jaxrs.methods.MKCOL;
import net.java.dev.webdav.jaxrs.methods.MOVE;
import net.java.dev.webdav.jaxrs.methods.PROPFIND;
import net.java.dev.webdav.jaxrs.methods.PROPPATCH;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.webdav.backend.Backend;

/**
 * Resource for an unknown (ie non-existing) object. Used so that PUT / MKCOL requests can actually created a document /
 * folder. Other requests will end up with a 404 error.
 */
public class UnknownResource extends AbstractResource {

    private static final String DS_STORE = ".DS_Store";

    protected Backend backend;

    public UnknownResource(String path, HttpServletRequest request, Backend backend) {
        super(path, request);
        this.backend = backend;
    }

    /**
     * PUT over a non-existing resource: create a new file resource.
     */
    @PUT
    public Response put() throws IOException, URISyntaxException {

        // Special case: ignore some Mac OS X files.
        // ._ files cannot easily be skipped as the Finder requires them on creation
        // We only forbid .DS_Store creation for now.
        if (DS_STORE.equals(name)) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }

        ensureParentExists();
        String contentType = request.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        Blob content = Blobs.createBlob(request.getInputStream(), contentType, null);
        content.setFilename(name);
        backend.createFile(parentPath, name, content);
        backend.saveChanges();
        return Response.created(new URI(request.getRequestURI())).build();
    }

    /**
     * MKCOL over a non-existing resource: create a new folder resource.
     */
    @MKCOL
    public Response mkcol() throws IOException, URISyntaxException {
        ensureParentExists();
        backend.createFolder(parentPath, name);
        backend.saveChanges();
        return Response.created(new URI(request.getRequestURI())).build();
    }

    // All these methods should raise an error 404

    @DELETE
    public Response delete() {
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @COPY
    public Response copy() {
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @MOVE
    public Response move() {
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @PROPFIND
    public Response propfind() {
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @PROPPATCH
    public Response proppatch() {
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    @HEAD
    public Response head() {
        throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    // Utility

    private void ensureParentExists() {
        if (!backend.exists(parentPath)) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
    }

}
