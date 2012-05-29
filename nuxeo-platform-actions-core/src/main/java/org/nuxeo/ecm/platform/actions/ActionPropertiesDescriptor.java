/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: PropertiesDescriptor.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.actions;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties = new HashMap<String, String>();

    @XNodeMap(value = "propertyList", key = "@name", type = HashMap.class, componentType = ActionPropertyListDescriptor.class)
    Map<String, ActionPropertyListDescriptor> listProperties = new HashMap<String, ActionPropertyListDescriptor>();

    @XNodeMap(value = "propertyMap", key = "@name", type = HashMap.class, componentType = ActionPropertiesDescriptor.class)
    Map<String, ActionPropertiesDescriptor> mapProperties = new HashMap<String, ActionPropertiesDescriptor>();

    public HashMap<String, Serializable> getProperties() {
        HashMap<String, Serializable> map = new HashMap<String, Serializable>();
        map.putAll(properties);
        for (Map.Entry<String, ActionPropertyListDescriptor> prop : listProperties.entrySet()) {
            map.put(prop.getKey(), prop.getValue().getValues());
        }
        for (Map.Entry<String, ActionPropertiesDescriptor> prop : mapProperties.entrySet()) {
            map.put(prop.getKey(), prop.getValue().getProperties());
        }
        return map;
    }

}
