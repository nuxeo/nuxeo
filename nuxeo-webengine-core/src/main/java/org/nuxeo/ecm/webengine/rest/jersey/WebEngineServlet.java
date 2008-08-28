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

package org.nuxeo.ecm.webengine.rest.jersey;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;

import org.nuxeo.ecm.webengine.rest.jersey.patch.ServletContainer;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.WebApplication;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineServlet extends ServletContainer {

    private static final long serialVersionUID = 1L;


    @Override
    protected WebApplication create() {
        return new WebEngineApplication();
    }

    protected ContainerRequest createContainerRequest(HttpServletRequest request,
            WebApplication _application, URI baseUri, URI requestUri) throws IOException {
        // remove action from uri
        String path = requestUri.getPath();
        int p = path.lastIndexOf("@@");
        String action = null;
        if (p > -1) {
            action = path.substring(p+2);
            path = path.substring(0, p);
            // remove @@ that is specific to webengine
            requestUri = UriBuilder.fromUri(
                    requestUri).replacePath(path).build();
        }
        return new org.nuxeo.ecm.webengine.rest.jersey.patch.ServletContainerRequest(
                request,
                _application,
                request.getMethod(),
                baseUri,
                requestUri,
                getHeaders(request),
                request.getInputStream(), action);
    }

}
