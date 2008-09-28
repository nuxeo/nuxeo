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

import java.io.InputStream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.resteasy.util.HttpRequestImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineHttpRequest extends HttpRequestImpl {

    protected WebContext2 ctx;
    
    public WebEngineHttpRequest(WebContext2 ctx, HttpHeaders httpHeaders, InputStream inputStream, UriInfo uri, String httpMethod) {
       super(inputStream, httpHeaders, httpMethod, uri);
       this.ctx = ctx;
    }
    
    public WebContext2 getContext() {
        return this.ctx;
    }

}
