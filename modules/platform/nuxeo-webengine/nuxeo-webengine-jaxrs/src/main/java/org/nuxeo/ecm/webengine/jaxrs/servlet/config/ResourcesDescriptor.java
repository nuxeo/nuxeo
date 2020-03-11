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

import java.net.URL;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.osgi.framework.Bundle;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("resources")
public class ResourcesDescriptor {

    @XNode("@servlet")
    protected String servlet;

    @XNode("@path")
    protected String path;

    private Bundle bundle;

    private String id;

    private ResourceResolver resolver;

    void setBundle(Bundle bundle) {
        this.bundle = bundle;
        if (path.startsWith("file:")) {
            resolver = new FileResourceResolver(path.substring("file:".length()));
        } else {
            resolver = new BundleResourceResolver(bundle, path);
        }
        id = bundle.getSymbolicName() + ":" + servlet + ":" + path;
    }

    public void setResolver(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getPath() {
        return path;
    }

    public String getServlet() {
        return servlet;
    }

    public String getId() {
        return id;
    }

    public URL getResource(String name) {
        return resolver.getResource(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResourcesDescriptor) {
            return id.equals(((ResourcesDescriptor) obj).id);
        }
        return false;
    }

    @Override
    public String toString() {
        return id;
    }
}
