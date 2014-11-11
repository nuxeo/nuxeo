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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.View;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleResource {

    public final static String VIEW_ROOT = "VROOT";

    protected ResourceContext context;

    public BundleResource() {
    }

    public BundleResource(ResourceContext context) {
        setContext(context);
    }

    public BundleResource setContext(ResourceContext context) {
        this.context = context;
        return this;
    }

    public ResourceContext getContext() {
        return context;
    }

    public final Bundle getBundle() {
        return context.getBundle();
    }

    public final RenderingEngine getRenderingEngine() {
        return context.getRenderingEngine();
    }

    public final View getView(String path) {
        String basePath = context.getUriInfo().getBaseUri().toString();
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() -1);
        }
        // we need to prefix with bundle name to avoid template cache collisions (in freemarker for ex.)
        //path=getBundle().getSymbolicName() + ":/" + path;
        return context.getRenderingEngine().getView(path, this)
        .arg("baseUrl", basePath).arg(VIEW_ROOT, context.getViewRoot());
    }

    public final HttpServletRequest getRequest() {
        return context.getRequest();
    }

    public CoreSession getSession() {
        return context.getSession();
    }

    public Principal getPrincipal() {
        return context.getPrincipal();
    }

    public UriInfo getUriInfo() {
        return context.getUriInfo();
    }

    public URI getBaseUri() {
        return context.getBaseUri();
    }

    public <T extends BundleResource> T getResource(Class<T> clazz) {
        try {
            T res = clazz.newInstance();
            res.setContext(context);
            return res;
        } catch (Exception e) {
            throw new WebApplicationException(e, 500);
        }
    }

    @Path("{any}")
    public Object dispatch(@PathParam("any") String segment) {
        BundleResource res = context.getApplication().getExtension(this, segment);
        if (res != null) {
            return res;
        }
        throw new WebApplicationException(404);
    }

    /**
     * This method is only for contributed sub-resources. It will be ignored for root resources.
     *
     * Extension resources may override this method to dynamically accept or reject
     * to be installed as a sub-resource of the target resource
     * @param target
     * @return
     */
    public boolean accept(BundleResource target) {
        return true;
    }

}
