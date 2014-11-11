/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Descriptor for virtual users.
 *
 * @author Anahide Tchertchian
 */
@XObject("virtualUser")
public class VirtualUserDescriptor implements VirtualUser {

    private static final long serialVersionUID = 1L;

    @XNode("@id")
    protected String id;

    @XNode("@remove")
    protected boolean remove;

    // searchable by default
    @XNode("@searchable")
    protected boolean searchable = true;

    @XNode("password")
    protected String password;

    // XXX for now only dealing with String properties
    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> properties;

    @XNodeMap(value = "propertyList", key = "@name", type = HashMap.class, componentType = PropertyListDescriptor.class)
    protected Map<String, PropertyListDescriptor> listProperties = new HashMap<String, PropertyListDescriptor>();

    @XNodeList(value = "group", type = ArrayList.class, componentType = String.class)
    protected List<String> groups;

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public Map<String, Serializable> getProperties() {
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.putAll(properties);
        for (Map.Entry<String, PropertyListDescriptor> prop : listProperties.entrySet()) {
            props.put(prop.getKey(), prop.getValue().getValues());
        }
        return props;
    }

    public List<String> getGroups() {
        return groups;
    }

    public boolean isSearchable() {
        return searchable;
    }

}
