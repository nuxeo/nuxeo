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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.webdav.Util;
import org.nuxeo.ecm.webdav.provider.CoreSessionWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Providers;
import java.security.Principal;

@Path("dav")
public class RootResource {

    static String rootPath;

    private static final Log log = LogFactory.getLog(RootResource.class);

    private CoreSession session;

    private HttpServletRequest request;

    public RootResource(@Context CoreSession session, @Context HttpServletRequest request,
            @Context CloseableService closeableService) {
        this.session = session;
        this.request = request;
        closeableService.add(new CoreSessionWrapper(session));

        log.info("Session = " + session);
        log.info(request.getMethod() + " " + request.getRequestURI());
        if (rootPath == null) {
            rootPath = request.getContextPath() + request.getServletPath();
            log.info(rootPath);
        }
    }

    @GET
    @Produces("text/plain")
    public String get() {
        return "OK";
    }

    @GET
    @Path("debug")
    @Produces("text/plain")
    public String debug() throws Exception {
        Principal principal = session.getPrincipal();

        String result = "DEBUG:\n\n"
            + "Principal (from request) = " + request.getUserPrincipal() + "\n"
            + "Principal = " + (principal == null ? "null" : principal.getName()) + "\n";

        return result;
    }

    @Path("{path:.+}")
    public Object findResource(@PathParam("path") String path) throws Exception {
        DocumentRef ref = new PathRef("/" + path);
        if (!session.exists(ref)) {
            Util.endTransaction();
            return new UnknownResource(path, request, session);
        }

        // Send 401 error if not authorised to read.
        if (!session.hasPermission(ref, SecurityConstants.READ)) {
            Util.endTransaction();
            return Response.status(401);
        }

        DocumentModel doc = session.getDocument(ref);
        if (doc.isFolder()) {
            return new FolderResource(doc, request);
        } else {
            return new FileResource(doc, request);
        }
    }

}
