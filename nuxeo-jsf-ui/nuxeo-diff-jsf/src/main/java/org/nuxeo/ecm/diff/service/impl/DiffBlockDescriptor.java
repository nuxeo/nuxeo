/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.descriptors.PropertiesDescriptor;
import org.nuxeo.ecm.platform.forms.layout.descriptors.WidgetDescriptor;

/**
 * Diff block descriptor.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
@XObject("diffBlock")
public class DiffBlockDescriptor {

    @XNode("@name")
    public String name;

    @XNodeMap(value = "templates/template", key = "@mode", type = HashMap.class, componentType = String.class)
    Map<String, String> templates = new HashMap<>();

    @XNodeList(value = "fields/field", type = ArrayList.class, componentType = DiffFieldDescriptor.class)
    public List<DiffFieldDescriptor> fields;

    @XNodeMap(value = "properties", key = "@mode", type = HashMap.class, componentType = PropertiesDescriptor.class)
    Map<String, PropertiesDescriptor> properties = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplate(String mode) {
        String template = templates.get(mode);
        if (template == null) {
            template = templates.get(BuiltinModes.ANY);
        }
        return template;
    }

    public Map<String, String> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, String> templates) {
        this.templates = templates;
    }

    public List<DiffFieldDescriptor> getFields() {
        return fields;
    }

    public void setFields(List<DiffFieldDescriptor> fields) {
        this.fields = fields;
    }

    public Map<String, Serializable> getProperties(String layoutMode) {
        return WidgetDescriptor.getProperties(properties, layoutMode);
    }

    public Map<String, Map<String, Serializable>> getProperties() {
        return WidgetDescriptor.getProperties(properties);
    }

    public void setProperties(Map<String, PropertiesDescriptor> properties) {
        this.properties = properties;
    }
}
