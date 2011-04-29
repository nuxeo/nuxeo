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

import java.net.URI;
import java.security.Principal;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.osgi.framework.Bundle;

/**
 * A resource request context.
 * This class is not thread safe.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceContext {

    protected ServletContext servletContext;

    protected HttpServletRequest request;

    protected UriInfo uriInfo;

    private Bundle bundle;

    private RenderingEngine rendering;

    private CoreSession session;

    protected ResourceContext() {
    }

    public ResourceContext(ServletContext servletContext, HttpServletRequest request, UriInfo uriInfo) {
        this.servletContext = servletContext;
        this.request = request;
        this.uriInfo = uriInfo;
    }

    public final Bundle getBundle() {
        if (bundle == null) {
            bundle = (Bundle)servletContext.getAttribute(Bundle.class.getName());
        }
        return bundle;
    }

    public final RenderingEngine getRenderingEngine() {
        if (rendering == null) {
            rendering = (RenderingEngine)servletContext.getAttribute(RenderingEngine.class.getName());
            String baseUrl = getBaseUri().toString();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length()-1);
            }
            rendering.setSharedVariable("baseUrl", baseUrl);
        }
        return rendering;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public Principal getPrincipal() {
        return request.getUserPrincipal();
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public CoreSession getSession() {
        if (session == null) {
            session = SessionFactory.getSession(request);
        }
        return session;
    }

    public URI getBaseUri() {
        return uriInfo.getBaseUri();
    }

}
