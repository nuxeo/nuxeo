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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetDefinitionImpl;

/**
 * Default implementation of a {@link DiffBlockDefinition}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class DiffBlockDefinitionImpl implements DiffBlockDefinition {

    private static final long serialVersionUID = 511776842683091931L;

    protected String name;

    protected Map<String, String> templates;

    protected List<DiffFieldDefinition> fields;

    protected Map<String, Map<String, Serializable>> properties;

    public DiffBlockDefinitionImpl(String name, Map<String, String> templates, List<DiffFieldDefinition> fields,
            Map<String, Map<String, Serializable>> properties) {
        this.name = name;
        this.templates = templates;
        if (fields == null) {
            this.fields = new ArrayList<>();
        } else {
            this.fields = fields;
        }
        this.properties = properties;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTemplate(String mode) {
        if (templates != null) {
            String template = templates.get(mode);
            if (template == null) {
                template = templates.get(BuiltinModes.ANY);
            }
            return template;
        }
        return null;
    }

    @Override
    public Map<String, String> getTemplates() {
        return templates;
    }

    @Override
    public List<DiffFieldDefinition> getFields() {
        return fields;
    }

    @Override
    public Map<String, Serializable> getProperties(String layoutMode) {
        return WidgetDefinitionImpl.getProperties(properties, layoutMode);
    }

    @Override
    public Map<String, Map<String, Serializable>> getProperties() {
        return properties;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof DiffBlockDefinition)) {
            return false;
        }

        String otherName = ((DiffBlockDefinition) other).getName();
        if (name == null && otherName == null) {
            return true;
        }
        if (name == null || otherName == null || !name.equals(otherName)) {
            return false;
        }

        Map<String, String> otherTemplates = ((DiffBlockDefinition) other).getTemplates();
        List<DiffFieldDefinition> otherFields = ((DiffBlockDefinition) other).getFields();
        Map<String, Map<String, Serializable>> otherProperties = ((DiffBlockDefinition) other).getProperties();
        if (MapUtils.isEmpty(templates) && MapUtils.isEmpty(otherTemplates) && CollectionUtils.isEmpty(fields)
                && CollectionUtils.isEmpty(otherFields) && MapUtils.isEmpty(properties)
                && MapUtils.isEmpty(otherProperties)) {
            return true;
        }
        if (MapUtils.isEmpty(templates) && !MapUtils.isEmpty(otherTemplates) || !MapUtils.isEmpty(templates)
                && MapUtils.isEmpty(otherTemplates) || (templates != null && !templates.equals(otherTemplates))
                || CollectionUtils.isEmpty(fields) && !CollectionUtils.isEmpty(otherFields)
                || !CollectionUtils.isEmpty(fields) && CollectionUtils.isEmpty(otherFields)
                || (fields != null && !fields.equals(otherFields)) || MapUtils.isEmpty(properties)
                && !MapUtils.isEmpty(otherProperties) || !MapUtils.isEmpty(properties)
                && MapUtils.isEmpty(otherProperties) || (properties != null && !properties.equals(otherProperties))) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return name + fields + templates + properties;
    }
}
