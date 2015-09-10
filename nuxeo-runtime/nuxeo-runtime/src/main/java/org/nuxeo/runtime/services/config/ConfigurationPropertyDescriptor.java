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
 *      Anahide Tchertchian
 */
package org.nuxeo.runtime.services.config;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for JSF configuration contributions.
 *
 * @since 7.4
 */
@XObject("property")
public class ConfigurationPropertyDescriptor {

    @XNode("@name")
    protected String name;

    @XNode
    protected String value;

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public ConfigurationPropertyDescriptor clone() {
        ConfigurationPropertyDescriptor clone = new ConfigurationPropertyDescriptor();
        clone.name = name;
        clone.value = value;
        return clone;
    }

    public void merge(ConfigurationPropertyDescriptor other) {
        value = other.value;
    }
}
