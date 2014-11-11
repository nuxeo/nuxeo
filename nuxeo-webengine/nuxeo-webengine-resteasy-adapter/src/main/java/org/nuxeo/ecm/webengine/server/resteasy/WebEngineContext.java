/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.server.resteasy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.resteasy.spi.HttpRequest;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.impl.AbstractWebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineContext extends AbstractWebContext {// extends HttpRequestImpl implements WebContext2 {

    private static final Log log = LogFactory.getLog(WebContext.class);

    protected UriInfo uri;
    protected final HttpRequest jaxReq;

    public WebEngineContext(HttpRequest jaxReq, HttpServletRequest request) {
        super(request);
        this.jaxReq = jaxReq;
    }

    public HttpServletRequest getHttpServletRequest() {
        return request;
    }

    public UriInfo getUriInfo() {
        return jaxReq.getUri();
    }

    public HttpHeaders getHttpHeaders() {
        return jaxReq.getHttpHeaders();
    }

    public HttpRequest getHttpRequest() {
        return jaxReq;
    }
}
