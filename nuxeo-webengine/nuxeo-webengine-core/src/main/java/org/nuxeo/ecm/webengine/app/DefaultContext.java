/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.webengine.model.impl.AbstractWebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultContext extends AbstractWebContext {

    // FIXME: these two members must be removed - they are redundant and buggy
    protected UriInfo info;
    protected HttpHeaders headers;

    public DefaultContext(HttpServletRequest request) {
        super(request);
    }

    @Deprecated
    public HttpHeaders getHttpHeaders() {
        //throw new UnsupportedOperationException("Deprecated. Use @Context HttpHeaders to inject this object");
        return headers;
    }

    @Deprecated
    public UriInfo getUriInfo() {
        //throw new UnsupportedOperationException("Deprecated. Use @Context UriInfo to inject this object");
        return info;
    }

    public void setUriInfo(UriInfo info) {
        this.info = info;
    }

    public void setHttpHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

}
