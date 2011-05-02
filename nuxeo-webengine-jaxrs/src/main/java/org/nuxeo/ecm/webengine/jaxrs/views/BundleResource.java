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

    protected ResourceContext context;

    public BundleResource() {
    }

    public BundleResource(ResourceContext context) {
        this.context = context;
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
        .arg("baseUrl", basePath);
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

    //    @Path("{any}")
    //    public Object dispatch(@PathParam("any") String segment) {
    //        ResourceFactory rf = ResourceManager.get(getClass(), segment);
    //        Object o = rf.newResource(this);
    //        if (o != null) {
    //            return o;
    //        }
    //        throw new WebApplicationException(404);
    //    }

}
