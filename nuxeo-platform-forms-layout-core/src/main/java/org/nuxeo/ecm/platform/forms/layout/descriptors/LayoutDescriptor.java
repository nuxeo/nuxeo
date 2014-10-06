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
 * $Id: LayoutDescriptor.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.descriptors;

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
     * @since 5.9.6
     */
    @XNode("@type")
    String type;

    /**
     * @since 5.9.6
     */
    @XNode("@typeCategory")
    String typeCategory;

    @XNodeMap(value = "templates/template", key = "@mode", type = HashMap.class, componentType = String.class)
    Map<String, String> templates = new HashMap<String, String>();

    @XNodeList(value = "rows/row", type = LayoutRowDescriptor[].class, componentType = LayoutRowDescriptor.class)
    LayoutRowDescriptor[] rows = new LayoutRowDescriptor[0];

    @XNodeList(value = "columns/column", type = LayoutRowDescriptor[].class, componentType = LayoutRowDescriptor.class)
    LayoutRowDescriptor[] rowsAsColumns = new LayoutRowDescriptor[0];

    @XNodeMap(value = "widget", key = "@name", type = HashMap.class, componentType = WidgetDescriptor.class)
    Map<String, WidgetDescriptor> widgets = new HashMap<String, WidgetDescriptor>();

    @XNodeMap(value = "properties", key = "@mode", type = HashMap.class, componentType = PropertiesDescriptor.class)
    Map<String, PropertiesDescriptor> properties = new HashMap<String, PropertiesDescriptor>();

    @XNodeMap(value = "renderingInfos", key = "@mode", type = HashMap.class, componentType = RenderingInfosDescriptor.class)
    Map<String, RenderingInfosDescriptor> renderingInfos = new HashMap<String, RenderingInfosDescriptor>();

    @XNodeList(value = "categories/category", type = String[].class, componentType = String.class)
    String[] categories = new String[0];

    /**
     * @since 5.9.6
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
     * Returns the categories for this layout, so that it can be stored in the
     * corresponding registries.
     *
     * @since 5.5
     */
    public String[] getCategories() {
        return categories;
    }

    /**
     * @since 5.9.6
     */
    public List<String> getAliases() {
        return aliases;
    }

    public LayoutDefinition getLayoutDefinition() {
        Map<String, String> ctemplates = null;
        if (templates != null) {
            ctemplates = new HashMap<String, String>();
            ctemplates.putAll(templates);
        }
        LayoutRowDefinition[] crows = getRows();
        Map<String, WidgetDefinition> cwidgets = null;
        if (widgets != null) {
            cwidgets = new HashMap<String, WidgetDefinition>();
            for (Map.Entry<String, WidgetDescriptor> entry : widgets.entrySet()) {
                WidgetDescriptor w = entry.getValue();
                cwidgets.put(entry.getKey(), getWidgetDefinition(w));
            }
        }
        Map<String, List<RenderingInfo>> crenderingInfos = null;
        if (renderingInfos != null) {
            crenderingInfos = new HashMap<String, List<RenderingInfo>>();
            for (Map.Entry<String, RenderingInfosDescriptor> item : renderingInfos.entrySet()) {
                RenderingInfosDescriptor infos = item.getValue();
                List<RenderingInfo> clonedInfos = null;
                if (infos != null) {
                    clonedInfos = new ArrayList<RenderingInfo>();
                    for (RenderingInfoDescriptor info : infos.getRenderingInfos()) {
                        clonedInfos.add(info.getRenderingInfo());
                    }
                }
                crenderingInfos.put(item.getKey(), clonedInfos);
            }
        }
        LayoutDefinitionImpl clone = new LayoutDefinitionImpl(name,
                getProperties(), ctemplates, crows, cwidgets);
        clone.setRenderingInfos(crenderingInfos);
        clone.setType(getType());
        clone.setTypeCategory(getTypeCategory());
        if (aliases != null) {
            clone.setAliases(new ArrayList<String>(aliases));
        }
        return clone;
    }
}
