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
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

/**
 * A HttpContext that delegates resource lookup to contributed {@link ResourcesDescriptor}
 * in the inverse order of the contribution (preserving the ordering imposed by extension mechanism)
 * <p>
 * A BundleHttpContext is created for every declated servlet when it is registered against to HttpService.
 * The context is removed when the servlet is unregistered.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleHttpContext implements HttpContext {

    protected Bundle bundle;
    protected ResourceResolver resolver;

    protected volatile ResourcesDescriptor[] resources;

    public BundleHttpContext(Bundle bundle, String resourcesPath) {
        this.bundle = bundle;
        if (resourcesPath != null) {
            if (resourcesPath.startsWith("file:")) {
                resolver = new FileResourceResolver(resourcesPath.substring("file:".length()));
            } else {
                resolver = new BundleResourceResolver(bundle, resourcesPath);
            }
        }
    }

    public void setResources(ResourcesDescriptor[] resources) {
        this.resources = resources;
    }

    @Override
    public String getMimeType(String name) {
        return null;
    }

    @Override
    public URL getResource(String name) {
        ResourcesDescriptor[] _resources = resources;
        if (_resources != null) {
            for (int i=_resources.length-1; i>=0; i--) {
                URL url = _resources[i].getResource(name);
                if (url != null) {
                    return url;
                }
            }
        }
        if (resolver != null) {
            return resolver.getResource(name);
        }
        return null;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        // default behaviour assumes the container has already performed authentication
        return true;
    }
}
