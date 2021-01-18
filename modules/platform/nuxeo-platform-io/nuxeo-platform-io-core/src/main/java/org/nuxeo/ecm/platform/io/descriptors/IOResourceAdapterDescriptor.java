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
 */

package org.nuxeo.ecm.platform.io.descriptors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.platform.io.api.IOResourceAdapter;

/**
 * Resource adapter descriptor.
 */
@XObject("adapter")
@XRegistry
public class IOResourceAdapterDescriptor {

    @XNode("@name")
    @XRegistryId
    String name;

    @XNode("@class")
    Class<? extends IOResourceAdapter> klass;

    // single properties map
    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties;

    @XNodeMap(value = "properties", key = "@name", type = HashMap.class, componentType = PropertyListDescriptor.class)
    Map<String, PropertyListDescriptor> listProperties;

    public String getName() {
        return name;
    }

    /** @since 11.5 */
    public Class<? extends IOResourceAdapter> getKlass() {
        return klass;
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
