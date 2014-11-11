/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.relations.descriptors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.runtime.api.Framework;

/**
 * Graph descriptor.
 */
@XObject("graph")
public class GraphDescriptor implements GraphDescription, Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    public String name;

    @XNode("@type")
    public String graphType;

    public Map<String, String> options = new HashMap<String, String>();

    @XNodeMap(value = "namespaces/namespace", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> namespaces = new HashMap<String, String>();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGraphType() {
        return graphType;
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public void setOptions(Map<String, String> options) {
        // expand vars on the options
        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            String value = entry.getValue();
            map.put(entry.getKey(), Framework.getRuntime().expandVars(value));
        }
        this.options = map;
    }

    @Override
    public Map<String, String> getNamespaces() {
        return namespaces;
    }

}
