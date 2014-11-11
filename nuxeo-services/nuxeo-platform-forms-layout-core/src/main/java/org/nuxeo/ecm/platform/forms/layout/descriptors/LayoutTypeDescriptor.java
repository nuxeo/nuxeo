/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
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
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutTypeDefinitionImpl;

/**
 * @since 6.0
 */
@XObject("layoutType")
public class LayoutTypeDescriptor {

    @XNode("@name")
    String name;

    @XNodeList(value = "aliases/alias", type = ArrayList.class, componentType = String.class)
    List<String> aliases;

    @XNodeMap(value = "templates/template", key = "@mode", type = HashMap.class, componentType = String.class)
    Map<String, String> templates = new HashMap<String, String>();

    @XNode("configuration")
    LayoutTypeConfigurationDescriptor configuration;

    @XNodeList(value = "categories/category", type = String[].class, componentType = String.class)
    String[] categories = new String[0];

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public Map<String, String> getTemplates() {
        return templates;
    }

    public LayoutTypeConfiguration getConfiguration() {
        if (configuration == null) {
            return null;
        }
        return configuration.getLayoutTypeConfiguration();
    }

    public String[] getCategories() {
        return categories;
    }

    public LayoutTypeDefinition getLayoutTypeDefinition() {
        LayoutTypeDefinitionImpl res = new LayoutTypeDefinitionImpl(name,
                templates, getConfiguration());
        res.setAliases(getAliases());
        return res;
    }

}
