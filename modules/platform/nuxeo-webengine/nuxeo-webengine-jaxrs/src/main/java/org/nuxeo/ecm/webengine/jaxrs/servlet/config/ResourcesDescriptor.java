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
 *     Bogdan Stefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet.config;

import java.net.URL;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodes;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.runtime.model.impl.XMapContext;
import org.osgi.framework.Bundle;

/**
 * Descriptor for resource resolvers that can be contributed to a servlet.
 */
@XObject("resources")
@XRegistry(compatWarnOnMerge = true)
@XRegistryId({ "@servlet", "@path" })
public class ResourcesDescriptor {

    /** @since 11.5 */
    @XNode
    protected Context ctx;

    /** @since 11.5 */
    @XNodes(values = { "@servlet", "@path" })
    protected String resolverId;

    @XNode("@servlet")
    protected String servlet;

    @XNode("@path")
    protected String path;

    private ResourceResolver resolver;

    public Bundle getBundle() {
        if (ctx instanceof XMapContext) {
            return ((XMapContext) ctx).getRuntimeContext().getBundle();
        }
        return null;
    }

    public String getPath() {
        return path;
    }

    public String getServlet() {
        return servlet;
    }

    public String getId() {
        Bundle bundle = getBundle();
        return bundle != null ? bundle.getSymbolicName() : "" + ":" + resolverId;
    }

    protected ResourceResolver getResolver() {
        if (resolver == null) {
            if (path != null && path.startsWith("file:")) {
                resolver = new FileResourceResolver(path.substring("file:".length()));
            } else {
                resolver = new BundleResourceResolver(getBundle(), path);
            }
        }
        return resolver;
    }

    public URL getResource(String name) {
        return getResolver().getResource(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResourcesDescriptor) {
            return getId().equals(((ResourcesDescriptor) obj).getId());
        }
        return false;
    }

    @Override
    public String toString() {
        return getId();
    }

}
