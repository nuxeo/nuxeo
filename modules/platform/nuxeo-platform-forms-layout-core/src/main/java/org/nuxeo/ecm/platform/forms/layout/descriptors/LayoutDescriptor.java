/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: LayoutDescriptor.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.RenderingInfo;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutDefinitionImpl;

/**
 * Layout definition descriptor.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("layout")
public class LayoutDescriptor {

    @XNode("@name")
    String name;

    /**
     * @since 6.0
     */
    @XNode("@type")
    String type;

    /**
     * @since 6.0
     */
    @XNode("@typeCategory")
    String typeCategory;

    @XNodeMap(value = "templates/template", key = "@mode", type = HashMap.class, componentType = String.class)
    Map<String, String> templates = new HashMap<>();

    @XNodeList(value = "rows/row", type = LayoutRowDescriptor[].class, componentType = LayoutRowDescriptor.class)
    LayoutRowDescriptor[] rows = new LayoutRowDescriptor[0];

    @XNodeList(value = "columns/column", type = LayoutRowDescriptor[].class, componentType = LayoutRowDescriptor.class)
    LayoutRowDescriptor[] rowsAsColumns = new LayoutRowDescriptor[0];

    @XNodeMap(value = "widget", key = "@name", type = HashMap.class, componentType = WidgetDescriptor.class)
    Map<String, WidgetDescriptor> widgets = new HashMap<>();

    @XNodeMap(value = "properties", key = "@mode", type = HashMap.class, componentType = PropertiesDescriptor.class)
    Map<String, PropertiesDescriptor> properties = new HashMap<>();

    @XNodeMap(value = "renderingInfos", key = "@mode", type = HashMap.class, componentType = RenderingInfosDescriptor.class)
    Map<String, RenderingInfosDescriptor> renderingInfos = new HashMap<>();

    @XNodeList(value = "categories/category", type = String[].class, componentType = String.class)
    String[] categories = new String[0];

    /**
     * @since 6.0
     */
    @XNodeList(value = "aliases/alias", type = ArrayList.class, componentType = String.class)
    List<String> aliases;

    Integer columns;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getTypeCategory() {
        return typeCategory;
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

    protected LayoutRowDefinition[] getDefinitions(LayoutRowDescriptor[] rows) {
        LayoutRowDefinition[] crows = null;
        if (rows != null) {
            crows = new LayoutRowDefinition[rows.length];
            for (int i = 0; i < rows.length; i++) {
                crows[i] = rows[i].getLayoutRowDefinition();
            }
        }
        return crows;
    }

    public LayoutRowDefinition[] getRows() {
        // check if columns tags are used instead of rows, they act as aliases.
        if (rowsAsColumns != null && rowsAsColumns.length > 0) {
            return getDefinitions(rowsAsColumns);
        }
        return getDefinitions(rows);
    }

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

    protected WidgetDefinition getWidgetDefinition(WidgetDescriptor desc) {
        if (desc == null) {
            return null;
        }
        return desc.getWidgetDefinition();
    }

    public WidgetDefinition getWidgetDefinition(String name) {
        return getWidgetDefinition(widgets.get(name));
    }

    public Map<String, Serializable> getProperties(String layoutMode) {
        return WidgetDescriptor.getProperties(properties, layoutMode);
    }

    public Map<String, Map<String, Serializable>> getProperties() {
        return WidgetDescriptor.getProperties(properties);
    }

    /**
     * Returns the categories for this layout, so that it can be stored in the corresponding registries.
     *
     * @since 5.5
     */
    public String[] getCategories() {
        return categories;
    }

    /**
     * @since 6.0
     */
    public List<String> getAliases() {
        return aliases;
    }

    public LayoutDefinition getLayoutDefinition() {
        Map<String, String> ctemplates = null;
        if (templates != null) {
            ctemplates = new HashMap<>();
            ctemplates.putAll(templates);
        }
        LayoutRowDefinition[] crows = getRows();
        Map<String, WidgetDefinition> cwidgets = null;
        if (widgets != null) {
            cwidgets = new LinkedHashMap<>();
            for (Map.Entry<String, WidgetDescriptor> entry : widgets.entrySet()) {
                WidgetDescriptor w = entry.getValue();
                cwidgets.put(entry.getKey(), getWidgetDefinition(w));
            }
        }
        Map<String, List<RenderingInfo>> crenderingInfos = null;
        if (renderingInfos != null) {
            crenderingInfos = new HashMap<>();
            for (Map.Entry<String, RenderingInfosDescriptor> item : renderingInfos.entrySet()) {
                RenderingInfosDescriptor infos = item.getValue();
                List<RenderingInfo> clonedInfos = null;
                if (infos != null) {
                    clonedInfos = new ArrayList<>();
                    for (RenderingInfoDescriptor info : infos.getRenderingInfos()) {
                        clonedInfos.add(info.getRenderingInfo());
                    }
                }
                crenderingInfos.put(item.getKey(), clonedInfos);
            }
        }
        LayoutDefinitionImpl clone = new LayoutDefinitionImpl(name, getProperties(), ctemplates, crows, cwidgets);
        clone.setRenderingInfos(crenderingInfos);
        clone.setType(getType());
        clone.setTypeCategory(getTypeCategory());
        if (aliases != null) {
            clone.setAliases(new ArrayList<>(aliases));
        }
        return clone;
    }
}
