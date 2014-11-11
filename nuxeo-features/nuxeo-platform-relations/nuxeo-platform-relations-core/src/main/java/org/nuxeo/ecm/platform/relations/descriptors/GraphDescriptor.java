/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: GraphDescriptor.java 20140 2007-06-06 17:53:55Z fguillaume $
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
 * Graph extension.
 * <p>
 * A new type of graph can implement this class to set XMap annotations for
 * custom options (?).
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("graph")
public class GraphDescriptor implements GraphDescription, Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    private String name;

    @XNode("@type")
    private String graphType;

    private Map<String, String> options = new HashMap<String, String>();

    @XNodeMap(value = "namespaces/namespace", key = "@name", type = HashMap.class, componentType = String.class)
    private Map<String, String> namespaces = new HashMap<String, String>();

    public String getName() {
        return name;
    }

    public String getGraphType() {
        return graphType;
    }

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

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

}
