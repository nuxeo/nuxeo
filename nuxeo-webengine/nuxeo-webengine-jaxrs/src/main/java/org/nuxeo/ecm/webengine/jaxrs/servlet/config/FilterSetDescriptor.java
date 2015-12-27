/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.webengine.jaxrs.servlet.FilterSet;
import org.nuxeo.ecm.webengine.jaxrs.servlet.mapping.Path;
import org.nuxeo.ecm.webengine.jaxrs.servlet.mapping.PathMatcher;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("filters")
public class FilterSetDescriptor {

    /**
     * To be used only when a filter set is declared outside a servlet. This is the ID of the contributed filter set.
     */
    @XNode("@id")
    protected String id;

    /**
     * To be used only when a filter set is declared outside a servlet. This is the target servlet name where the filter
     * should be added.
     */
    @XNode("@target")
    protected String targetServlet;

    @XNodeList(value = "filter", type = ArrayList.class, componentType = FilterDescriptor.class, nullByDefault = false)
    protected ArrayList<FilterDescriptor> filters;

    @XNode("@pathInfo")
    public void setPathInfo(String pathInfo) {
        path = PathMatcher.compile(pathInfo);
    }

    private PathMatcher path;

    public PathMatcher getPath() {
        return path;
    }

    public List<FilterDescriptor> getFilters() {
        return filters;
    }

    public FilterSet getFilterSet() {
        return new FilterSet(this);
    }

    public boolean matches(String pathInfo) {
        if (path == null) {
            return true;
        }
        if (pathInfo == null || pathInfo.length() == 0) {
            pathInfo = "/";
        }
        return path.matches(pathInfo);
    }

    public boolean matches(Path pathInfo) {
        if (path == null) {
            return true;
        }
        return path.matches(pathInfo);
    }

    public String getId() {
        return id;
    }

    public String getTargetServlet() {
        return targetServlet;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (id != null) {
            buf.append(id).append("@").append(targetServlet).append(": ");
        }
        String p = path == null ? "/**" : path.toString();
        buf.append(p).append(" ").append(filters);
        return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof FilterSetDescriptor) {
            String objId = ((FilterSetDescriptor) obj).id;
            if (objId == null && this.id == null) {
                return super.equals(obj);
            }
            if (objId != null) {
                return objId.equals(this.id);
            }
        }
        return false;
    }
}
