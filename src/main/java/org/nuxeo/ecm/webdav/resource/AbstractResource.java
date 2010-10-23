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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webdav.Util;
import org.nuxeo.ecm.webdav.locking.LockManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * Base class for all resources.
 */
public class AbstractResource {

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

}
