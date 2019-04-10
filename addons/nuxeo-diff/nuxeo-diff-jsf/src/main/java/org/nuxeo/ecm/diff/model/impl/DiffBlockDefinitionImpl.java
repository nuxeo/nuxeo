/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
            this.fields = new ArrayList<DiffFieldDefinition>();
        } else {
            this.fields = fields;
        }
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

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

    public Map<String, String> getTemplates() {
        return templates;
    }

    public List<DiffFieldDefinition> getFields() {
        return fields;
    }

    public Map<String, Serializable> getProperties(String layoutMode) {
        return WidgetDefinitionImpl.getProperties(properties, layoutMode);
    }

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
        if (name == null && otherName != null || name != null && otherName == null || !name.equals(otherName)) {
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
