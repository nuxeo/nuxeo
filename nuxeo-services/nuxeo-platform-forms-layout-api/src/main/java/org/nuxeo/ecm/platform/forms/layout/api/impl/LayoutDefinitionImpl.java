/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.RenderingInfo;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;

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

    protected String type;

    protected String typeCategory;

    protected Map<String, Map<String, Serializable>> properties;

    protected Map<String, String> templates;

    protected LayoutRowDefinition[] rows;

    protected Map<String, WidgetDefinition> widgets;

    protected Map<String, List<RenderingInfo>> renderingInfos;

    protected Integer columns;

    protected List<String> aliases;

    protected boolean dynamic = false;

    // needed by GWT serialization
    protected LayoutDefinitionImpl() {
        super();
    }

    public LayoutDefinitionImpl(String name, String template, WidgetDefinition widgetDefinition) {
        super();
        this.name = name;
        this.properties = null;
        this.templates = new HashMap<>();
        if (template != null) {
            this.templates.put(BuiltinModes.ANY, template);
        }
        this.widgets = new HashMap<>();
        if (widgetDefinition != null) {
            this.widgets.put(widgetDefinition.getName(), widgetDefinition);
            this.rows = new LayoutRowDefinition[] { new LayoutRowDefinitionImpl(null, widgetDefinition.getName()) };
        } else {
            this.rows = new LayoutRowDefinition[0];
        }
    }

    public LayoutDefinitionImpl(String name, Map<String, Map<String, Serializable>> properties,
            Map<String, String> templates, List<LayoutRowDefinition> rows, List<WidgetDefinition> widgetDefinitions) {
        super();
        this.name = name;
        this.properties = properties;
        this.templates = templates;
        if (rows == null) {
            this.rows = new LayoutRowDefinition[0];
        } else {
            this.rows = rows.toArray(new LayoutRowDefinition[0]);
        }
        this.widgets = new HashMap<>();
        if (widgetDefinitions != null) {
            for (WidgetDefinition widgetDef : widgetDefinitions) {
                this.widgets.put(widgetDef.getName(), widgetDef);
            }
        }
    }

    public LayoutDefinitionImpl(String name, Map<String, Map<String, Serializable>> properties,
            Map<String, String> templates, LayoutRowDefinition[] rows, Map<String, WidgetDefinition> widgets) {
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
            columns = Integer.valueOf(0);
            LayoutRowDefinition[] rows = getRows();
            for (LayoutRowDefinition def : rows) {
                int current = def.getWidgetReferences().length;
                if (current > columns.intValue()) {
                    columns = Integer.valueOf(current);
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
    public void setName(String name) {
        this.name = name;
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
    public Map<String, Serializable> getProperties(String layoutMode) {
        return WidgetDefinitionImpl.getProperties(properties, layoutMode);
    }

    @Override
    public Map<String, Map<String, Serializable>> getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Map<String, Map<String, Serializable>> properties) {
        this.properties = properties;
    }

    @Override
    public LayoutRowDefinition[] getRows() {
        return rows;
    }

    @Override
    public void setRows(LayoutRowDefinition[] rows) {
        this.rows = rows;
    }

    /**
     * @since 6.0
     */
    public static String getTemplate(Map<String, String> templates, String mode) {
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
    public String getTemplate(String mode) {
        return getTemplate(templates, mode);
    }

    @Override
    public Map<String, String> getTemplates() {
        return templates;
    }

    @Override
    public void setTemplates(Map<String, String> templates) {
        this.templates = templates;
    }

    @Override
    public WidgetDefinition getWidgetDefinition(String name) {
        if (widgets != null) {
            return widgets.get(name);
        }
        return null;
    }

    @Override
    public Map<String, WidgetDefinition> getWidgetDefinitions() {
        return widgets;
    }

    @Override
    public Map<String, List<RenderingInfo>> getRenderingInfos() {
        return renderingInfos;
    }

    @Override
    public void setRenderingInfos(Map<String, List<RenderingInfo>> renderingInfos) {
        this.renderingInfos = renderingInfos;
    }

    @Override
    public List<RenderingInfo> getRenderingInfos(String mode) {
        return WidgetDefinitionImpl.getRenderingInfos(renderingInfos, mode);
    }

    @Override
    public boolean isEmpty() {
        LayoutRowDefinition[] rows = getRows();
        if (rows == null) {
            return true;
        }
        for (LayoutRowDefinition row : rows) {
            WidgetReference[] refs = row.getWidgetReferences();
            if (refs != null) {
                for (WidgetReference ref : refs) {
                    if (ref.getName() != null && !ref.getName().isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    @Override
    public LayoutDefinition clone() {
        Map<String, Map<String, Serializable>> cprops = null;
        if (properties != null) {
            cprops = new HashMap<>();
            for (Map.Entry<String, Map<String, Serializable>> entry : properties.entrySet()) {
                Map<String, Serializable> subProps = entry.getValue();
                Map<String, Serializable> csubProps = null;
                if (subProps != null) {
                    csubProps = new HashMap<>();
                    csubProps.putAll(subProps);
                }
                cprops.put(entry.getKey(), csubProps);
            }
        }
        Map<String, String> ctemplates = null;
        if (templates != null) {
            ctemplates = new HashMap<>();
            ctemplates.putAll(templates);
        }
        LayoutRowDefinition[] crows = null;
        if (rows != null) {
            crows = new LayoutRowDefinition[rows.length];
            for (int i = 0; i < rows.length; i++) {
                crows[i] = rows[i].clone();
            }
        }
        Map<String, WidgetDefinition> cwidgets = null;
        if (widgets != null) {
            cwidgets = new LinkedHashMap<>();
            for (Map.Entry<String, WidgetDefinition> entry : widgets.entrySet()) {
                WidgetDefinition w = entry.getValue();
                if (w != null) {
                    w = w.clone();
                }
                cwidgets.put(entry.getKey(), w);
            }
        }
        Map<String, List<RenderingInfo>> crenderingInfos = null;
        if (renderingInfos != null) {
            crenderingInfos = new HashMap<>();
            for (Map.Entry<String, List<RenderingInfo>> item : renderingInfos.entrySet()) {
                List<RenderingInfo> infos = item.getValue();
                List<RenderingInfo> clonedInfos = null;
                if (infos != null) {
                    clonedInfos = new ArrayList<>();
                    for (RenderingInfo info : infos) {
                        clonedInfos.add(info.clone());
                    }
                }
                crenderingInfos.put(item.getKey(), clonedInfos);
            }
        }
        LayoutDefinitionImpl clone = new LayoutDefinitionImpl(name, cprops, ctemplates, crows, cwidgets);
        clone.setRenderingInfos(crenderingInfos);
        clone.setType(type);
        clone.setTypeCategory(typeCategory);
        if (aliases != null) {
            clone.setAliases(new ArrayList<>(aliases));
        }
        clone.setDynamic(dynamic);
        return clone;
    }

    /**
     * @since 7.2
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LayoutDefinitionImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        LayoutDefinitionImpl ld = (LayoutDefinitionImpl) obj;
        return new EqualsBuilder().append(name, ld.name).append(type, ld.type).append(typeCategory, ld.typeCategory).append(
                properties, ld.properties).append(templates, ld.templates).append(rows, ld.rows).append(widgets,
                ld.widgets).append(renderingInfos, ld.renderingInfos).append(aliases, ld.aliases).append(dynamic,
                ld.dynamic).isEquals();
    }

}
