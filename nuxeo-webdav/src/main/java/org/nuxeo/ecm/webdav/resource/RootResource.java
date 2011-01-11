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

import com.sun.jersey.spi.CloseableService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.webdav.backend.NuxeoWebDavBackend;
import org.nuxeo.ecm.webdav.backend.WebDavBackend;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("dav")
public class RootResource {

    /**
     * Global root path computed once to serve as a prefix to URLs when needed.
     */
    static String rootPath;

    private static final Log log = LogFactory.getLog(RootResource.class);

    private HttpServletRequest request;

    private WebDavBackend backend;

    public RootResource(@Context HttpServletRequest request,
                        @Context CloseableService closeableService) throws Exception {
        log.debug(request.getMethod() + " " + request.getRequestURI());

        this.request = request;

        if (rootPath == null) {
            rootPath = request.getContextPath() + request.getServletPath();
            log.info(rootPath);
        }

        backend = new NuxeoWebDavBackend("/", rootPath);
    }

    @GET
    @Produces("text/html")
    public Object getRoot() throws Exception {
        return ((FolderResource) findResource("")).get();
    }

    @Path("{path:.+}")
    public Object findResource(@PathParam("path") String path) throws Exception {

        DocumentModel doc = null;
        try {
            path = new String(path.getBytes(), "UTF-8");
            doc = backend.resolveLocation(path);
        } catch (Exception e) {
            log.error("Error during resolving path: " + path, e);
        }

        if (doc == null) {
            return new UnknownResource(path, request, backend);
        }

        // Send 401 error if not authorised to read.
        if (!backend.getSession().hasPermission(doc.getRef(), SecurityConstants.READ)) {
            return Response.status(401);
        }

        if (doc.isFolder()) {
            return new FolderResource(doc, request, backend);
        } else {
            return new FileResource(doc, request, backend);
        }
    }

}
