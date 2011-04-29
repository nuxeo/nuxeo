/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.views;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RootResource extends BundleResource {

    public RootResource() {
        super (new ResourceContext());
    }

    @Context
    public void setRequest(HttpServletRequest request) {
        this.context.request = request;
    }

    @Context
    public void setServletContext(ServletContext servletContext) {
        this.context.servletContext = servletContext;
    }

    @Context
    public void setUriInfo(UriInfo uriInfo) {
        this.context.uriInfo = uriInfo;
    }

}