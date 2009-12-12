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

package org.nuxeo.ecm.webdav;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import net.java.dev.webdav.core.jaxrs.methods.MKCOL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.services.streaming.InputStreamSource;

@WebObject(type = "WebDAV")
public class Main extends ModuleRoot {

    private static final Log log = LogFactory.getLog(Main.class);

    @GET
    @Path("{path:.+}")
    public Response get(@PathParam("path") String path) throws ClientException,
            IOException {
        log.info("get called for " + path);
        CoreSession session = ctx.getCoreSession();

        DocumentRef ref = new PathRef("/" + path);
        if (!session.exists(ref)) {
            return Response.status(404).entity("Nothing found at " + ref).build();
        }
        DocumentModel doc = session.getDocument(ref);
        if (doc.getDeclaredFacets().contains("Folderish")) {
            return Response.ok("Not a file").build();
        }
        Blob content = (Blob) doc.getPropertyValue("file:content");
        if (content == null) {
            return Response.ok("").build();
        } else {
            return Response.ok(content.getStream()).type(content.getMimeType()).build();
        }
    }

    @DELETE
    @Path("{path:.+}")
    public Response delete(@PathParam("path") String path) throws ClientException {
        log.debug("delete called for " + path);
        CoreSession session = ctx.getCoreSession();

        DocumentRef ref = new PathRef(path);
        if (!session.exists(ref)) {
            return Response.status(404).build();
        }

        session.removeDocument(ref);
        session.save();
        return Response.ok().build();
    }

    @MKCOL
    @Path("{path:.+}")
    public Response mkcol(@PathParam("path") String path) throws ClientException, URISyntaxException {
        log.info("mkcol called for " + path);

        CoreSession session = ctx.getCoreSession();

        DocumentRef ref = new PathRef(path);
        if (session.exists(ref)) {
            return Response.status(405).build();
        }

        org.nuxeo.common.utils.Path p = new org.nuxeo.common.utils.Path(path);
        String parentPath = p.removeLastSegments(1).toString();
        String folderName = p.lastSegment().toString();

        DocumentRef parentRef = new PathRef("/" + parentPath);

        if (!session.exists(parentRef)) {
            return Response.status(409).build();
        }

        DocumentModel folder = new DocumentModelImpl(parentPath, folderName, "Folder");
        folder = session.createDocument(folder);
        session.save();
        return Response.created(new URI(path)).build();
    }

    @PUT
    @Path("{path:.+}")
    public Response createObject(@PathParam("path") String path)
            throws ClientException, IOException, URISyntaxException {
        log.debug("createObject called on " + path);

        CoreSession session = ctx.getCoreSession();
        ServletRequest request = ctx.getRequest();

        org.nuxeo.common.utils.Path p = new org.nuxeo.common.utils.Path(path);
        String parentPath = p.removeLastSegments(1).toString();
        String filename = p.lastSegment().toString();

        DocumentRef parentRef = new PathRef(parentPath);
        if (!session.exists(parentRef)) {
            return Response.status(409).build();
        }

        Blob content = new StreamingBlob(new InputStreamSource(request.getInputStream()));
        content.setMimeType(request.getContentType());
        content.setFilename(filename);

        DocumentModel newdoc = session.createDocumentModel(parentPath, filename, "File");

        newdoc.getProperty("file:content").setValue(content);
        newdoc = session.createDocument(newdoc);
        session.save();

        return Response.created(new URI(path)).build();
    }

}
