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

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodes;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.webengine.jaxrs.ApplicationManager;
import org.nuxeo.ecm.webengine.jaxrs.views.BundleResource;
import org.nuxeo.runtime.model.impl.XMapContext;
import org.osgi.framework.Bundle;

/**
 * Descriptor for a resource extension, defining sub-resources that can be injected into the target application.
 */
@XObject("resource")
@XRegistry(compatWarnOnMerge = true)
@XRegistryId(value = { "@target", "@segment" }, separator = "#")
public class ResourceExtension {

    /** @since 11.5 */
    @XNode
    protected Context ctx;

    @XNode("@class")
    protected Class<? extends BundleResource> clazz;

    /** @since 11.5 */
    @XNodes(values = { "@target", "@segment" }, separator = "#")
    protected String id;

    @XNode("@target")
    protected String target;

    @XNode("@segment")
    protected String segment;

    @XNode(value = "@application", defaultAssignment = ApplicationManager.DEFAULT_HOST)
    protected String application;

    public Bundle getBundle() {
        if (ctx instanceof XMapContext) {
            return ((XMapContext) ctx).getRuntimeContext().getBundle();
        }
        return null;
    }

    public String getTarget() {
        return target;
    }

    public String getSegment() {
        return segment;
    }

    public String getApplication() {
        return application;
    }

    public Class<? extends BundleResource> getResourceClass() {
        return clazz;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        Bundle bundle = getBundle();
        return bundle != null ? bundle.getSymbolicName() : "" + ":" + id;
    }

}
