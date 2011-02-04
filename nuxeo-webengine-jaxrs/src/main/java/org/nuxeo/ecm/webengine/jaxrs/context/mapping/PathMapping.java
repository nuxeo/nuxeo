/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.context.mapping;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("mapping")
public final class PathMapping implements Comparable<PathMapping> {

    protected String path;
    protected PathMatcher matcher;

    @XNodeMap(value="property", key="@name", type=HashMap.class, componentType=String.class)
    protected Map<String, String> params;


    public PathMapping() {
    }

    @XNode("path")
    protected void setPath(String path) {
        this.path = path;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public PathMatcher getMatcher() {
        if (matcher == null) {
            matcher = PathMatcher.compile(path);
        }
        return matcher;
    }

    public String getPath() {
        return path;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() == PathMapping.class) {
            return ((PathMapping)obj).path.equals(path);
        }
        return false;
    }

    @Override
    public int compareTo(PathMapping o) {
        return path.compareTo(o.path);
    }
}
