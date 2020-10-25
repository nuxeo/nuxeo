/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for virtual users. APG-240 All attributes are defined public because the user manager service do not get
 * access to the fields. OSGI don't allow splitted packages having access to public members defined from an another
 * package provider.
 *
 * @author Anahide Tchertchian
 */
@XObject("virtualUser")
public class VirtualUserDescriptor implements VirtualUser {

    @XNode("@id")
    public String id;

    @XNode("@remove")
    public boolean remove;

    // searchable by default
    @XNode("@searchable")
    public boolean searchable = true;

    @XNode("password")
    public String password;

    // XXX for now only dealing with String properties
    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties;

    @XNodeMap(value = "propertyList", key = "@name", type = HashMap.class, componentType = PropertyListDescriptor.class)
    public Map<String, PropertyListDescriptor> listProperties = new HashMap<>();

    @XNodeList(value = "group", type = ArrayList.class, componentType = String.class)
    public List<String> groups;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Map<String, Serializable> getProperties() {
        Map<String, Serializable> props = new HashMap<>();
        props.putAll(properties);
        for (Map.Entry<String, PropertyListDescriptor> prop : listProperties.entrySet()) {
            props.put(prop.getKey(), prop.getValue().getValues());
        }
        return props;
    }

    @Override
    public List<String> getGroups() {
        return groups;
    }

    @Override
    public boolean isSearchable() {
        return searchable;
    }

}
