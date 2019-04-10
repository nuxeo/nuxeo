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

import net.java.dev.webdav.jaxrs.methods.LOCK;
import net.java.dev.webdav.jaxrs.methods.UNLOCK;
import net.java.dev.webdav.jaxrs.xml.elements.*;
import net.java.dev.webdav.jaxrs.xml.properties.LockDiscovery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webdav.Util;
import org.nuxeo.ecm.webdav.locking.LockManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 * Base class for all resources (existing or not).
 */
public class AbstractResource {

    private static final Log log = LogFactory.getLog(AbstractResource.class);

    protected String path;
    protected String parentPath;
    protected String name;

    protected HttpServletRequest request;
    protected CoreSession session;

    protected LockManager lockManager = LockManager.getInstance();

    protected AbstractResource(String path, HttpServletRequest request, CoreSession session) throws Exception {
        this.path = path;
        this.request = request;
        this.session = session;
        parentPath = Util.getParentPath(path);
        name = Util.getNameFromPath(path);
    }

    @OPTIONS
    public Response options() throws Exception {
        return Response.status(204).entity("")
            .header("DAV", "1,2") // not 1,2 for now.
            .header("Allow", "GET, HEAD, POST, PUT, DELETE, OPTIONS, TRACE, "
                    + "PROPFIND, PROPPATCH, MKCOL, COPY, MOVE, LOCK, UNLOCK").build();
    }

    // Note: some clients like to lock/unlock non-existent resources.

    @LOCK
    public Response lock() throws Exception {
        String token = Util.getTokenFromHeaders("if", request);
        if (lockManager.isLocked(path) && !lockManager.canUnlock(path, token)) {
            return Response.status(423).build();
        }

        LockInfo lockInfo;
        if (request.getHeader("content-length") != null) {
            try {
                Unmarshaller u = Util.getUnmarshaller();
                lockInfo = (LockInfo) u.unmarshal(request.getInputStream());
                //Util.printAsXml(lockInfo);
                token = lockManager.lock(path);
            } catch (JAXBException e) {
                log.error(e);
                // FIXME: check this is the right response code
                return Response.status(400).build();
            }
        } else if (token != null) {
            // OK
        } else {
            return Response.status(400).build();
        }

        Prop prop = new Prop(new LockDiscovery(new ActiveLock(
                LockScope.EXCLUSIVE, LockType.WRITE, Depth.ZERO,
                new Owner("toto"),
                new TimeOut(10000L), new LockToken(new HRef("urn:uuid:" + token)),
                new LockRoot(new HRef("http://asdasd/"))
        )));
        return Response.ok().entity(prop)
                .header("Lock-Token", "urn:uuid:" + token).build();
    }

    @UNLOCK
    public Response unlock() {
        if (lockManager.isLocked(path)) {
            String token = Util.getTokenFromHeaders("lock-token", request);
            if (!lockManager.canUnlock(path, token)) {
                return Response.status(423).build();
            }
            lockManager.unlock(path);
            return Response.status(204).build();
        } else {
            // TODO: return an error
            return Response.status(204).build();
        }
    }

}
