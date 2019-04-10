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
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.LockDiscovery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webdav.Constants;
import org.nuxeo.ecm.webdav.Util;
import org.nuxeo.ecm.webdav.locking.LockManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.net.URI;

/**
 * An existing resource corresponds to an existing object (folder or file)
 * in the repository.
 */
public class ExistingResource extends AbstractResource {

    private static final Log log = LogFactory.getLog(ExistingResource.class);

    protected DocumentModel doc;

    protected ExistingResource(DocumentModel doc, HttpServletRequest request) throws Exception {
        super(doc.getPathAsString(), request, doc.getCoreSession());
        this.doc = doc;
        session = doc.getCoreSession();
    }

    @DELETE
    public Response delete() throws Exception {
        if (lockManager.isLocked(path)) {
            String token = Util.getTokenFromHeaders("if", request);
            if (!lockManager.canUnlock(path, token)) {
                return Response.status(423).build();
            }
        }

        DocumentRef ref = new PathRef(path);
        session.removeDocument(ref);
        session.save();
        session.destroy();
        return Response.status(204).build();
    }

    @COPY
    public Response copy(@HeaderParam("Destination") String dest,
            @HeaderParam("Overwrite") String overwrite) throws Exception {
        return copyOrMove("COPY", dest, overwrite);
    }

    @MOVE
    public Response move(@HeaderParam("Destination") String dest,
            @HeaderParam("Overwrite") String overwrite) throws Exception {
        if (lockManager.isLocked(path)) {
            String token = Util.getTokenFromHeaders("if", request);
            if (!lockManager.canUnlock(path, token)) {
                return Response.status(423).build();
            }
        }

        return copyOrMove("MOVE", dest, overwrite);
    }

    private Response copyOrMove(String method, @HeaderParam("Destination") String dest,
            @HeaderParam("Overwrite") String overwrite) throws Exception {
        URI destUri = new URI(dest);
        String destPath = destUri.getPath();
        while (destPath.endsWith("/")) {
            destPath = destPath.substring(0, destPath.length() - 1);
        }

        destPath = destPath.substring(
                RootResource.rootPath.length() + Constants.DAV_HOME.length(), destPath.length());
        log.info("to " + destPath);

        if (lockManager.isLocked(destPath)) {
            String token = Util.getTokenFromHeaders("if", request);
            if (!LockManager.getInstance().canUnlock(path, token)) {
                return Response.status(423).build();
            }
        }

        DocumentRef sourceRef = new PathRef(path);
        DocumentRef destRef = new PathRef(destPath);

        String destParentPath = Util.getParentPath(destPath);
        PathRef destParentRef = new PathRef(destParentPath);
        if (!session.exists(destParentRef)) {
            return Response.status(409).build();
        }

        // Remove dest if it exists and the Overwrite header is set to "T".
        int status = 201;
        if (session.exists(destRef)) {
            if ("F".equals(overwrite)) {
                return Response.status(412).build();
            }
            session.removeDocument(destRef);
            status = 204;
        }

        session.copy(sourceRef, destParentRef, Util.getNameFromPath(destPath));
        if ("MOVE".equals(method)) {
            session.removeDocument(sourceRef);
        }
        session.save();

        return Response.status(status).build();
    }

    // Properties

    @PROPPATCH
    public Response proppatch(@Context UriInfo uriInfo) throws Exception {
        if (lockManager.isLocked(path)) {
            return Response.status(423).build();
        }

        JAXBContext jc = Util.getJaxbContext();
        Unmarshaller u = jc.createUnmarshaller();
        PropertyUpdate propertyUpdate;
        try {
            propertyUpdate = (PropertyUpdate) u.unmarshal(request.getInputStream());
        } catch (JAXBException e) {
            return Response.status(400).build();
        }
        //Util.printAsXml(propertyUpdate);
        return Response.ok().build();
    }

    /**
     * We can't MKCOL over an existing resource.
     */
    @MKCOL
    public Response mkcol() {
        return Response.status(405).build();
    }

}
