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
import java.net.URL;
import java.security.Principal;
import java.util.LinkedList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.ApplicationHost;
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

    private static ThreadLocal<ResourceContext> perThreadContext = new ThreadLocal<ResourceContext>();

    public final static void setContext(ResourceContext context) {
        perThreadContext.set(context);
    }

    public final static ResourceContext getContext() {
        return perThreadContext.get();
    }

    public final static void destroyContext() {
        perThreadContext.remove();
    }

    /**
     * The JAX-RS application providing the resources.
     */
    protected ApplicationHost app;

    protected HttpServletRequest request;

    protected UriInfo uriInfo;

    private LinkedList<Bundle> bundleStack;

    private RenderingEngine rendering;

    private CoreSession session;


    protected ResourceContext() {
    }

    public ResourceContext(ApplicationHost app, RenderingEngine rendering) {
        //TODO rendering in app
        this.app = app;
        this.rendering = rendering;
        this.bundleStack = new LinkedList<Bundle>();
        //this.bundleStack.add(app.getBundle());
    }

    public final LinkedList<Bundle> getBundleStack() {
        return bundleStack;
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void setRendering(RenderingEngine rendering) {
        this.rendering = rendering;
    }

    public final Bundle getBundle() {
        return bundleStack.isEmpty() ? null : bundleStack.get(bundleStack.size()-1);
    }

    public final RenderingEngine getRenderingEngine() {
        //        if (rendering == null) {
        //            rendering = (RenderingEngine)servletContext.getAttribute(RenderingEngine.class.getName());
        //            String baseUrl = getBaseUri().toString();
        //            if (baseUrl.endsWith("/")) {
        //                baseUrl = baseUrl.substring(0, baseUrl.length()-1);
        //            }
        //            rendering.setSharedVariable("baseUrl", baseUrl);
        //        }
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


    public CoreSession getSession() {
        if (session == null) {
            session = SessionFactory.getSession(request);
        }
        return session;
    }

    public URI getBaseUri() {
        return uriInfo.getBaseUri();
    }

    public void pushBundleFor(Object obj) {
        Bundle b = getResourceBundle(obj);
        if (b != null) {
            pushBundle(b);
        }
    }

    public void pushBundle(Bundle bundle) {
        for (Bundle b : bundleStack) {
            if (b == bundle) {
                // already present
                return;
            }
        }
        bundleStack.add(bundle);
    }

    protected Bundle getResourceBundle(Object res) {
        //return FrameworkUtil.getBundle(res.getClass());
        return app.getBundle(res.getClass());
    }

    public URL findEntry(String path) {
        for (int i=bundleStack.size()-1; i>=0; i--) {
            URL url = bundleStack.get(i).getEntry(path);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

}
