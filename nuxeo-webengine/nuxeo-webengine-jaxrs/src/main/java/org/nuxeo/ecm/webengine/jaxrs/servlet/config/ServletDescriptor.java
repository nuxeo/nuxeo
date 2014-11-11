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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServlet;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.jaxrs.BundleNotFoundException;
import org.nuxeo.ecm.webengine.jaxrs.Utils;
import org.nuxeo.ecm.webengine.jaxrs.Utils.ClassRef;
import org.nuxeo.ecm.webengine.jaxrs.servlet.FilterSet;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("servlet")
public class ServletDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected String classRef;

    /**
     * The absolute path of the servlet (including the context path)
     */
    @XNode("@path")
    protected String path;

    /**
     * Must use hashtable since it extends Dictionary
     */
    @XNodeMap(value="properties/property", key="@name", type=HashMap.class, componentType=String.class, trim=true, nullByDefault=false)
    protected HashMap<String, String> initParams;

    @XNodeList(value="filters", type=ArrayList.class, componentType=FilterSetDescriptor.class, nullByDefault=false)
    protected ArrayList<FilterSetDescriptor> filters;

    @XNode("listeners")
    protected ListenerSetDescriptor listeners;

    private ClassRef ref;

    public ServletDescriptor() {
    }

    public String getPath() {
        return path;
    }

    public HashMap<String, String> getInitParams() {
        return initParams;
    }

    public ClassRef getClassRef() throws ClassNotFoundException, BundleNotFoundException {
        if (ref == null) {
            ref = Utils.getClassRef(classRef);
        }
        return ref;
    }

    public HttpServlet getServlet() throws Exception {
        return (HttpServlet)getClassRef().get().newInstance();
    }

    public String getName() {
        return name;
    }

    public ListenerSetDescriptor getListenerSet() {
        return listeners;
    }

    public FilterSet[] getFilters() throws Exception {
        List<FilterSetDescriptor> list = ServletRegistry.getInstance().getFiltersFor(name);
        int len1 = list.size();
        int len2 = filters.size();
        FilterSet[] filterSets = new FilterSet[len1+len2];
        for (int i=0; i<len1; i++) {
            filterSets[i] = list.get(i).getFilterSet();
        }
        for (int i=0; i<len2; i++) {
            filterSets[i+len1] = filters.get(i).getFilterSet();
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
