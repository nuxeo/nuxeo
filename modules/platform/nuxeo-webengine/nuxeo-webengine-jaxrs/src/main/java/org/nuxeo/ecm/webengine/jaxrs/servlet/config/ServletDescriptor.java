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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServlet;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.webengine.jaxrs.BundleNotFoundException;
import org.nuxeo.ecm.webengine.jaxrs.Utils;
import org.nuxeo.ecm.webengine.jaxrs.Utils.ClassRef;
import org.nuxeo.ecm.webengine.jaxrs.servlet.FilterSet;
import org.nuxeo.runtime.model.impl.XMapContext;
import org.osgi.framework.Bundle;

/**
 * Descriptor for a servlet contribution.
 */
@XObject("servlet")
@XRegistry(compatWarnOnMerge = true)
public class ServletDescriptor {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@class")
    protected String classRef;

    /**
     * The absolute path of the servlet (including the context path)
     */
    @XNode("@path")
    protected String path;

    @XNode("resources")
    protected String resources;

    @XNodeMap(value = "properties/property", key = "@name", type = HashMap.class, componentType = String.class, trim = true, nullByDefault = false)
    protected HashMap<String, String> initParams;

    @XNodeList(value = "filters", type = ArrayList.class, componentType = FilterSetDescriptor.class, nullByDefault = false)
    protected ArrayList<FilterSetDescriptor> filters;

    @XNode("listeners")
    protected ListenerSetDescriptor listeners;

    /** @since 11.5 */
    @XNode
    protected Context ctx;

    private ClassRef ref;

    public Bundle getBundle() {
        if (ctx instanceof XMapContext) {
            return ((XMapContext) ctx).getRuntimeContext().getBundle();
        }
        return null;
    }

    public String getPath() {
        return path;
    }

    public HashMap<String, String> getInitParams() {
        return new HashMap<>(initParams);
    }

    public ClassRef getClassRef() throws ClassNotFoundException, BundleNotFoundException {
        if (ref == null) {
            ref = Utils.getClassRef(classRef, getBundle());
        }
        return ref;
    }

    public HttpServlet getServlet() throws ReflectiveOperationException, BundleNotFoundException {
        return (HttpServlet) getClassRef().get().getDeclaredConstructor().newInstance();
    }

    public String getName() {
        return name;
    }

    public ListenerSetDescriptor getListenerSet() {
        return listeners;
    }

    public String getResources() {
        return resources;
    }

    public FilterSet[] getFilters() {
        List<FilterSetDescriptor> list = ServletRegistry.getInstance().getFiltersFor(name);
        int len1 = list.size();
        int len2 = filters.size();
        FilterSet[] filterSets = new FilterSet[len1 + len2];
        for (int i = 0; i < len1; i++) {
            filterSets[i] = list.get(i).getFilterSet();
        }
        for (int i = 0; i < len2; i++) {
            filterSets[i + len1] = filters.get(i).getFilterSet();
        }
        return filterSets;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(name).append(" { ").append(path).append(" [").append(classRef).append("]");
        buf.append("\n  Properties: ").append(initParams);
        if (!filters.isEmpty()) {
            buf.append("\n  Filters:\n    ");
            for (FilterSetDescriptor fsd : filters) {
                buf.append(fsd.toString());
                buf.append("\n    ");
            }
        }
        if (listeners != null) {
            buf.append("\n  Listeners: ").append(listeners.toString());
        }
        buf.append("\n}\n");
        return buf.toString();
    }

}
