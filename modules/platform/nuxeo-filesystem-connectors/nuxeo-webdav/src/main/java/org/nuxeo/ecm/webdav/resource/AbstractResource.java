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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webdav.resource;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.core.Response;

import org.nuxeo.common.utils.Path;

/**
 * Base class for all resources (existing or not).
 */
public class AbstractResource {

    protected String path;

    protected String parentPath;

    protected String name;

    protected HttpServletRequest request;

    public static String getParentPath(String path) {
        path = new Path(path).removeLastSegments(1).toString();
        // Ensures that path starts with a "/" and doesn't end with a "/".
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

    public static String getNameFromPath(String path) {
        return new Path(path).lastSegment();
    }

    protected AbstractResource(String path, HttpServletRequest request) {
        this.path = path;
        this.request = request;
        parentPath = getParentPath(path);
        name = getNameFromPath(path);
    }

    @OPTIONS
    public Response options() {
        return Response.status(204).entity("").header("DAV", "1,2") // not 1,2 for now.
        .header("Allow",
                "GET, HEAD, POST, PUT, DELETE, OPTIONS, TRACE, "
                        + "PROPFIND, PROPPATCH, MKCOL, COPY, MOVE, LOCK, UNLOCK").build();
    }

}
