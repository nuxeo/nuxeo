/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
