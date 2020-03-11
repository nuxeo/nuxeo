/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
    Map<String, String> properties = new HashMap<>();

    @XNodeMap(value = "properties", key = "@name", type = HashMap.class, componentType = PropertyListDescriptor.class)
    Map<String, PropertyListDescriptor> listProperties = new HashMap<>();

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public void setProperties(Map<String, String> properties) {
        Map<String, String> map = new HashMap<>();
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
        Map<String, Serializable> map = new HashMap<>();
        map.putAll(properties);
        for (Map.Entry<String, PropertyListDescriptor> prop : listProperties.entrySet()) {
            map.put(prop.getKey(), prop.getValue().getValues());
        }
        return map;
    }

}
