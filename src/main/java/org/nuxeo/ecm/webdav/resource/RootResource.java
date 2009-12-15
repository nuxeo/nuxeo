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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webdav.Util;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import java.security.Principal;

@Path("dav")
public class RootResource {

    private static final Log log = LogFactory.getLog(RootResource.class);

    public RootResource(@Context HttpServletRequest request) {
        log.info(request.getMethod() + " " + request.getRequestURI());
    }

    @GET
    @Produces("text/plain")
    public String get() {
        return "OK";
    }

    @GET
    @Path("debug")
    @Produces("text/plain")
    public String debug(@Context HttpServletRequest request) throws Exception {
        CoreSession session = Util.getSession(request);

        Principal principal = session.getPrincipal();

        String result = "DEBUG:\n\n"
            + "Principal (from request) = " + request.getUserPrincipal() + "\n"
            + "Principal = " + (principal == null ? "null" : principal.getName()) + "\n";

        return result;
    }

    @Path("{path:.+}")
    public Object findResource(@PathParam("path") String path,
            @Context HttpServletRequest request) throws Exception {

        CoreSession session = Util.getSession(request);

        DocumentRef ref = new PathRef("/" + path);
        if (!session.exists(ref)) {
            return new UnknownResource(path, request);
        }

        DocumentModel doc = session.getDocument(ref);
        if (doc.isFolder()) {
            return new FolderResource(path, doc, request);
        } else {
            return new FileResource(path, doc, request);
        }
    }

}
