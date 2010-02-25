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
 * $Id: PropertiesDescriptor.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Property descriptor
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("properties")
public class PropertiesDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties = new HashMap<String, String>();

    @XNodeMap(value = "propertyList", key = "@name", type = HashMap.class, componentType = PropertyListDescriptor.class)
    Map<String, PropertyListDescriptor> listProperties = new HashMap<String, PropertyListDescriptor>();

    public Map<String, Serializable> getProperties() {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.putAll(properties);
        for (Map.Entry<String, PropertyListDescriptor> prop : listProperties.entrySet()) {
            map.put(prop.getKey(), prop.getValue().getValues());
        }
        return map;
    }

}
