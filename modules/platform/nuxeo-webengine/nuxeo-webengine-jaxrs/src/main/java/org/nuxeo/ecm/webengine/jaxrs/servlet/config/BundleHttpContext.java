/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * A HttpContext that delegates resource lookup to contributed {@link ResourcesDescriptor} in the inverse order of the
 * contribution (preserving the ordering imposed by extension mechanism)
 * <p>
 * A BundleHttpContext is created for every declated servlet when it is registered against to HttpService. The context
 * is removed when the servlet is unregistered.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
            for (int i = _resources.length - 1; i >= 0; i--) {
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
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // default behaviour assumes the container has already performed authentication
        return true;
    }
}
