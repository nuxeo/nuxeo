/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *      Andre Justo
 */
package org.nuxeo.ecm.platform.ui.web.runtime;

import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Descriptor for JSF configuration contributions.
 *
 * @since 7.4
 */
@XObject("configuration")
public class JSFConfigurationDescriptor {

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> properties;

    public JSFConfigurationDescriptor() {}

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public JSFConfigurationDescriptor clone() {
        JSFConfigurationDescriptor clone = new JSFConfigurationDescriptor();
        doClone(clone);
        return clone;
    }

    protected void doClone(JSFConfigurationDescriptor clone) {
        clone.properties = new HashMap<>(properties);
    }

    public void merge(JSFConfigurationDescriptor other) {
        if (other.properties != null) {
            properties.putAll(other.properties);
        }
    }
}
