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
package org.nuxeo.runtime.jetty;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("servlet")
public class ServletDescriptor {

    // the filter name if any
    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected Class<?> clazz;

    @XNode("@context")
    protected String context;

    @XNode("@path")
    protected String path;

    @XNodeMap(value="init-params/param", key="@name", type=HashMap.class, componentType=String.class, trim=true, nullByDefault=true)
    protected Map<String, String> initParams;

    // the description if any
    @XNode("description")
    protected String description;


    public ServletDescriptor() {
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public String getContext() {
        return context;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getInitParams() {
        return initParams;
    }

    public void setInitParams(Map<String, String> initParams) {
        this.initParams = initParams;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
