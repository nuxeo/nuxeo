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

package org.nuxeo.ecm.webengine.rest.servlet.jersey;

import org.nuxeo.ecm.webengine.rest.WebContext2;
import org.nuxeo.ecm.webengine.rest.servlet.jersey.patch.WebApplicationContext;
import org.nuxeo.ecm.webengine.rest.servlet.jersey.patch.WebApplicationImpl;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineApplicationContext extends WebApplicationContext {

    protected WebContext2 ctx;
    
    public WebEngineApplicationContext(WebContext2 ctx, WebApplicationImpl app,
            ContainerRequest request, ContainerResponse response) {
        super (app, request, response);
        this.ctx = ctx;
    }
    
    public WebContext2 getContext() {
        return ctx;
    }
    
}
