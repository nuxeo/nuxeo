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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor for JSF configuration contributions.
 *
 * @since 7.4
 */
@XObject("property")
public class ConfigurationPropertyDescriptor implements Descriptor {

    public static final String SEPARATOR = ",";

    @XNode("@name")
    protected String name;

    @XNode("@append")
    public boolean append;

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
        clone.append = append;
        return clone;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        ConfigurationPropertyDescriptor other = (ConfigurationPropertyDescriptor) o;
        if (other.append) {
            ConfigurationPropertyDescriptor merged = new ConfigurationPropertyDescriptor();
            merged.append = other.append;
            merged.name = other.name != null ? other.name : name;
            if (StringUtils.isNotEmpty(value)) {
                merged.value = value + SEPARATOR + other.value;
            } else {
                merged.value = other.value;
            }
            return merged;
        } else {
            return other;
        }
    }

    @Override
    public String getId() {
        return getName();
    }

}
