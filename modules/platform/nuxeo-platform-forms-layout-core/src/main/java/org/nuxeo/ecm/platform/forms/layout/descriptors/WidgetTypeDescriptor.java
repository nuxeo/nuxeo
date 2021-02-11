/*
 * (C) Copyright 2006--2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeDefinitionImpl;

/**
 * Widget type descriptor.
 */
@XObject("widgetType")
@XRegistry(merge = false)
public class WidgetTypeDescriptor {

    @XNode("@name")
    @XRegistryId
    String name;

    /** @since 6.0 */
    @XNodeList(value = "aliases/alias", type = ArrayList.class, componentType = String.class)
    List<String> aliases;

    @XNode("handler-class")
    String handlerClassName;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties;

    /** @since 11.5: helper for JSF contributions */
    @XNode("@category")
    protected String category;

    @XNode("configuration")
    WidgetTypeConfigurationDescriptor configuration;

    @XNodeList(value = "categories/category", type = String[].class, componentType = String.class)
    String[] categories = new String[0];

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getHandlerClassName() {
        return handlerClassName;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public WidgetTypeConfiguration getConfiguration() {
        if (configuration == null) {
            return null;
        }
        return configuration.getWidgetTypeConfiguration();
    }

    /**
     * Returns the categories for this widget type, so that it can be stored in the corresponding registries.
     *
     * @since 5.5
     */
    public String[] getCategories() {
        Set<String> cats = new HashSet<>();
        if (category != null) {
            cats.add(category);
        }
        cats.addAll(Arrays.asList(categories));
        return cats.toArray(String[]::new);
    }

    public WidgetTypeDefinition getWidgetTypeDefinition() {
        WidgetTypeDefinitionImpl res = new WidgetTypeDefinitionImpl(name, handlerClassName, properties,
                getConfiguration());
        res.setAliases(getAliases());
        return res;
    }

}
