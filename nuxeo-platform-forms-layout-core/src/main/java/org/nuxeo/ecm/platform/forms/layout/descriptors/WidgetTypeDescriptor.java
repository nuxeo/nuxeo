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
 * $Id: WidgetTypeDescriptor.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeDefinitionImpl;

/**
 * Widget type descriptor.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("widgetType")
public class WidgetTypeDescriptor {

    @XNode("@name")
    String name;

    /**
     * @since 6.0
     */
    @XNodeList(value = "aliases/alias", type = ArrayList.class, componentType = String.class)
    List<String> aliases;

    @XNode("handler-class")
    String handlerClassName;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    Map<String, String> properties;

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
     * Returns the categories for this widget type, so that it can be stored in
     * the corresponding registries.
     *
     * @since 5.5
     */
    public String[] getCategories() {
        return categories;
    }

    public WidgetTypeDefinition getWidgetTypeDefinition() {
        WidgetTypeDefinitionImpl res = new WidgetTypeDefinitionImpl(name,
                handlerClassName, properties, getConfiguration());
        res.setAliases(getAliases());
        return res;

    }

}
