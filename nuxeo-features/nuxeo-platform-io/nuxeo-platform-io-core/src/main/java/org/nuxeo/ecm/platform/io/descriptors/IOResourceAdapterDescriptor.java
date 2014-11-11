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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: IOResourceAdapterDescriptor.java 24959 2007-09-14 13:46:47Z atchertchian $
 */

package org.nuxeo.ecm.platform.io.descriptors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

/**
 * Resource adapter descriptor
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("adapter")
public class IOResourceAdapterDescriptor {

    @XNode("@name")
    String name;

    @XNode("@class")
    String className;

    // single properties map
    Map<String, String> properties = new HashMap<String, String>();

    @XNodeMap(value = "properties", key = "@name", type = HashMap.class, componentType = PropertyListDescriptor.class)
    Map<String, PropertyListDescriptor> listProperties = new HashMap<String, PropertyListDescriptor>();

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public void setProperties(Map<String, String> properties) {
        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String value = entry.getValue();
            map.put(entry.getKey(), Framework.getRuntime().expandVars(value));
        }
        this.properties = map;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, Serializable> getProperties() {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.putAll(properties);
        for (Map.Entry<String, PropertyListDescriptor> prop : listProperties.entrySet()) {
            map.put(prop.getKey(), prop.getValue().getValues());
        }
        return map;
    }

}
