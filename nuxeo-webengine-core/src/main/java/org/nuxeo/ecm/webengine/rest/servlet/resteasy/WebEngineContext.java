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

package org.nuxeo.ecm.webengine.rest.servlet.resteasy;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.impl.AbstractWebContext;
import org.nuxeo.ecm.webengine.session.UserSession;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineContext extends AbstractWebContext {// extends HttpRequestImpl implements WebContext2 {

    protected static final Log log = LogFactory.getLog(WebContext2.class);

    protected HttpServletRequest request;


    public WebEngineContext(HttpServletRequest request) throws IOException {
        super (UserSession.getCurrentSession(request.getSession(true)));
        //super(request.getInputStream(), headers, httpMethod, uri);
        this.request = request;
    }
    
    public HttpServletRequest getHttpServletRequest() {
        return request;
    }

}
