/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;

/**
 * Default implementation for a layout definition.
 * <p>
 * Useful to compute layouts independently from the layout service.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class LayoutDefinitionImpl implements LayoutDefinition {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected Map<String, Map<String, Serializable>> properties;

    protected Map<String, String> templates;

    protected LayoutRowDefinition[] rows;

    protected Map<String, WidgetDefinition> widgets;

    protected Integer columns;

    // needed by GWT serialization
    protected LayoutDefinitionImpl() {
        super();
    }

    public LayoutDefinitionImpl(String name, String template,
            WidgetDefinition widgetDefinition) {
        super();
        this.name = name;
        this.properties = null;
        this.templates = new HashMap<String, String>();
        if (template != null) {
            this.templates.put(BuiltinModes.ANY, template);
        }
        this.widgets = new HashMap<String, WidgetDefinition>();
        if (widgetDefinition != null) {
            this.widgets.put(widgetDefinition.getName(), widgetDefinition);
            this.rows = new LayoutRowDefinition[] { new LayoutRowDefinitionImpl(
                    null, widgetDefinition.getName()) };
        } else {
            this.rows = new LayoutRowDefinition[0];
        }
    }

    public LayoutDefinitionImpl(String name,
            Map<String, Map<String, Serializable>> properties,
            Map<String, String> templates, List<LayoutRowDefinition> rows,
            List<WidgetDefinition> widgetDefinitions) {
        super();
        this.name = name;
        this.properties = properties;
        this.templates = templates;
        if (rows == null) {
            this.rows = new LayoutRowDefinition[0];
        } else {
            this.rows = rows.toArray(new LayoutRowDefinition[] {});
        }
        this.widgets = new HashMap<String, WidgetDefinition>();
        if (widgetDefinitions != null) {
            for (WidgetDefinition widgetDef : widgetDefinitions) {
                this.widgets.put(widgetDef.getName(), widgetDef);
            }
        }
    }

    public LayoutDefinitionImpl(String name,
            Map<String, Map<String, Serializable>> properties,
            Map<String, String> templates, LayoutRowDefinition[] rows,
            Map<String, WidgetDefinition> widgets) {
        super();
        this.name = name;
        this.properties = properties;
        this.templates = templates;
        this.rows = rows;
        this.widgets = widgets;
    }

    @Override
    public int getColumns() {
        if (columns == null) {
            // compute it
            columns = new Integer(0);
            LayoutRowDefinition[] rows = getRows();
            for (LayoutRowDefinition def : rows) {
                int current = def.getWidgets().length;
                if (current > columns.intValue()) {
                    columns = new Integer(current);
                }
            }
        }
        return columns.intValue();
    }

    @Override
    public String getName() {
        return name;
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
    public LayoutRowDefinition[] getRows() {
        return rows;
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
    public WidgetDefinition getWidgetDefinition(String name) {
        if (widgets != null) {
            return widgets.get(name);
        }
        return null;
    }

}
