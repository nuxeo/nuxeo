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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    protected static final Log log = LogFactory.getLog(ConfigurationPropertyDescriptor.class);

    @XNode("@name")
    protected String name;

    @XNode("@list")
    public boolean list;

    @XNode("@override")
    public boolean override;

    @XNode
    protected String value;

    @Override
    public ConfigurationPropertyDescriptor clone() {
        ConfigurationPropertyDescriptor clone = new ConfigurationPropertyDescriptor();
        clone.name = name;
        clone.value = value;
        clone.list = list;
        clone.override = override;
        return clone;
    }

    /**
     * @since 10.3
     */
    @Override
    public String getId() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    /**
     * @since 10.3
     */
    public boolean isList() {
        return list;
    }

    /**
     * @since 10.3
     */
    public boolean isOverride() {
        return override;
    }

    /**
     * @since 10.3
     */
    @Override
    public Descriptor merge(Descriptor o) {
        ConfigurationPropertyDescriptor other = (ConfigurationPropertyDescriptor) o;
        if (this.list) {
            ConfigurationPropertyDescriptor merged = new ConfigurationPropertyDescriptor();
            merged.list = this.list;
            merged.name = this.name;
            if (StringUtils.isNotEmpty(value) && !other.override) {
                merged.value = value + ConfigurationService.LIST_SEPARATOR + other.value;
            } else {
                merged.value = other.value;
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Merging property %s with old %s resulting in %s", other, this, merged));
            }
            return merged;
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Overriding existing property %s with %s", this, other));
            }
            if (other.list) {
                other.list = false;
                log.warn(String.format(
                        "Property %s cannot be marked as list because it is already defined as not list. Overriding existing property.",
                        other.getName()));
            }
            return other;
        }
    }

    /**
     * @since 10.3
     */
    @Override
    public String toString() {
        return String.format("name=%s, value=%s, list=%s, replace=%s", name, value, list, override);
    }

}
