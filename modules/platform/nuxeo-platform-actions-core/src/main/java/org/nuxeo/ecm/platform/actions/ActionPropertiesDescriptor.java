/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: PropertiesDescriptor.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Action property descriptor
 *
 * @since 5.6
 */
@XObject("properties")
public class ActionPropertiesDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@append")
    boolean append;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties = new HashMap<>();

    @XNodeMap(value = "propertyList", key = "@name", type = HashMap.class, componentType = ActionPropertyListDescriptor.class)
    Map<String, ActionPropertyListDescriptor> listProperties = new HashMap<>();

    @XNodeMap(value = "propertyMap", key = "@name", type = HashMap.class, componentType = ActionPropertiesDescriptor.class)
    Map<String, ActionPropertiesDescriptor> mapProperties = new HashMap<>();

    public HashMap<String, Serializable> getAllProperties() {
        HashMap<String, Serializable> map = new HashMap<>();
        map.putAll(properties);
        for (Map.Entry<String, ActionPropertyListDescriptor> prop : listProperties.entrySet()) {
            map.put(prop.getKey(), prop.getValue().getValues());
        }
        for (Map.Entry<String, ActionPropertiesDescriptor> prop : mapProperties.entrySet()) {
            map.put(prop.getKey(), prop.getValue().getAllProperties());
        }
        return map;
    }

    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, ActionPropertyListDescriptor> getListProperties() {
        return listProperties;
    }

    public void setListProperties(Map<String, ActionPropertyListDescriptor> listProperties) {
        this.listProperties = listProperties;
    }

    public Map<String, ActionPropertiesDescriptor> getMapProperties() {
        return mapProperties;
    }

    public void setMapProperties(Map<String, ActionPropertiesDescriptor> mapProperties) {
        this.mapProperties = mapProperties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void merge(ActionPropertiesDescriptor other) {
        if (other != null) {
            if (other.getProperties() != null) {
                if (properties == null) {
                    properties = other.getProperties();
                } else {
                    properties.putAll(other.getProperties());
                }
            }
            if (other.getListProperties() != null) {
                if (listProperties == null) {
                    listProperties = other.getListProperties();
                } else {
                    listProperties.putAll(other.getListProperties());
                }
            }
            if (other.getMapProperties() != null) {
                if (mapProperties == null) {
                    mapProperties = other.getMapProperties();
                } else {
                    mapProperties.putAll(other.getMapProperties());
                }
            }
        }
    }

    @Override
    public ActionPropertiesDescriptor clone() {
        ActionPropertiesDescriptor clone = new ActionPropertiesDescriptor();
        if (properties != null) {
            clone.properties = new HashMap<>(properties);
        }
        if (listProperties != null) {
            clone.listProperties = new HashMap<>();
            for (Map.Entry<String, ActionPropertyListDescriptor> item : listProperties.entrySet()) {
                clone.listProperties.put(item.getKey(), item.getValue().clone());
            }
        }
        if (mapProperties != null) {
            clone.mapProperties = new HashMap<>();
            for (Map.Entry<String, ActionPropertiesDescriptor> item : mapProperties.entrySet()) {
                clone.mapProperties.put(item.getKey(), item.getValue().clone());
            }
        }
        return clone;
    }

}
