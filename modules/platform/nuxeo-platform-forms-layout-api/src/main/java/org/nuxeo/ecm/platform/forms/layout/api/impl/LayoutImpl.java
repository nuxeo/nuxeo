/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;

/**
 * Implementation for layouts.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class LayoutImpl implements Layout {

    private static final long serialVersionUID = -8975637002024432963L;

    protected String id;

    protected String name;

    protected String type;

    protected String typeCategory;

    protected String mode;

    protected String template;

    protected String devTemplate;

    protected LayoutRow[] rows;

    protected Map<String, Widget> widgetMap;

    protected int columns = 0;

    protected Map<String, Serializable> properties;

    protected String definitionId;

    protected String valueName;

    protected boolean dynamic = false;

    protected LayoutDefinition definition;

    // needed by GWT serialization
    protected LayoutImpl() {
        super();
    }

    /**
     * @since 5.5
     */
    public LayoutImpl(String name, String mode, String template, List<LayoutRow> rows, int columns,
            Map<String, Serializable> properties, String definitionId) {
        this.name = name;
        this.mode = mode;
        this.template = template;
        this.rows = rows.toArray(new LayoutRow[0]);
        this.columns = columns;
        this.properties = properties;
        this.widgetMap = new HashMap<>();
        computeWidgetMap();
        this.definitionId = definitionId;
    }

    /**
     * @since 8.1
     */
    public LayoutImpl(String name, String mode, String template, Map<String, Widget> widgets,
            Map<String, Serializable> properties, String definitionId) {
        this.name = name;
        this.mode = mode;
        this.template = template;
        this.rows = new LayoutRow[0];
        this.columns = 0;
        this.properties = properties;
        this.widgetMap = new HashMap<>();
        if (widgets != null) {
            this.widgetMap.putAll(widgets);
        }
        this.definitionId = definitionId;
    }

    protected void computeWidgetMap() {
        if (rows == null || rows.length == 0) {
            return;
        }
        for (LayoutRow row : rows) {
            Widget[] widgets = row.getWidgets();
            if (widgets == null || widgets.length == 0) {
                continue;
            }
            for (Widget widget : widgets) {
                if (widget != null) {
                    widgetMap.put(widget.getName(), widget);
                }
            }
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTagConfigId() {
        StringBuilder builder = new StringBuilder();
        builder.append(definitionId).append(";");
        builder.append(mode).append(";");

        Integer intValue = Integer.valueOf(builder.toString().hashCode());
        return intValue.toString();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMode() {
        return mode;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public LayoutRow[] getRows() {
        return rows;
    }

    @Override
    public int getColumns() {
        return columns;
    }

    @Override
    public Widget getWidget(String name) {
        return widgetMap.get(name);
    }

    @Override
    public Map<String, Widget> getWidgetMap() {
        return Collections.unmodifiableMap(widgetMap);
    }

    @Override
    public Map<String, Serializable> getProperties() {
        if (properties == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public Serializable getProperty(String name) {
        if (properties != null) {
            return properties.get(name);
        }
        return null;
    }

    @Override
    public void setProperty(String name, Serializable value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(name, value);
    }

    @Override
    public String getValueName() {
        return valueName;
    }

    @Override
    public void setValueName(String valueName) {
        this.valueName = valueName;
        // set it on all widgets too
        if (rows == null || rows.length == 0) {
            return;
        }
        for (LayoutRow row : rows) {
            Widget[] widgets = row.getWidgets();
            if (widgets == null || widgets.length == 0) {
                continue;
            }
            for (Widget widget : widgets) {
                if (widget != null) {
                    widget.setValueName(valueName);
                }
            }
        }
    }

    /**
     * @since 6.0
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * @since 6.0
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @since 6.0
     */
    @Override
    public String getTypeCategory() {
        return typeCategory;
    }

    /**
     * @since 6.0
     */
    public void setTypeCategory(String typeCategory) {
        this.typeCategory = typeCategory;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("LayoutImpl");
        sb.append(" {");
        sb.append(" name=");
        sb.append(name);
        sb.append(", id=");
        sb.append(id);
        sb.append(", mode=");
        sb.append(mode);
        sb.append(", template=");
        sb.append(template);
        sb.append(", properties=");
        sb.append(properties);
        sb.append('}');

        return sb.toString();
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    @Override
    public String getDevTemplate() {
        return devTemplate;
    }

    public void setDevTemplate(String devTemplate) {
        this.devTemplate = devTemplate;
    }

    @Override
    public LayoutDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(LayoutDefinition definition) {
        this.definition = definition;
    }

}
