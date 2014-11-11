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
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;

/**
 * Layout definition descriptor.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
@XObject("layout")
public class LayoutDescriptor implements LayoutDefinition {

    private static final long serialVersionUID = 1386913375220603688L;

    @XNode("@name")
    String name;

    @XNodeMap(value = "templates/template", key = "@mode", type = HashMap.class, componentType = String.class)
    Map<String, String> templates = new HashMap<String, String>();

    @XNodeList(value = "rows/row", type = LayoutRowDescriptor[].class, componentType = LayoutRowDescriptor.class)
    LayoutRowDefinition[] rows = new LayoutRowDefinition[0];

    @XNodeMap(value = "widget", key = "@name", type = HashMap.class, componentType = WidgetDescriptor.class)
    Map<String, WidgetDefinition> widgets = new HashMap<String, WidgetDefinition>();

    @XNodeMap(value = "properties", key = "@mode", type = HashMap.class, componentType = PropertiesDescriptor.class)
    Map<String, PropertiesDescriptor> properties = new HashMap<String, PropertiesDescriptor>();

    String[][] stringRows;

    Integer columns;

    public String getName() {
        return name;
    }

    public String getTemplate(String mode) {
        String template = templates.get(mode);
        if (template == null) {
            template = templates.get(BuiltinModes.ANY);
        }
        return template;
    }

    public LayoutRowDefinition[] getRows() {
        return rows;
    }

    public int getColumns() {
        if (columns == null) {
            // compute it
            columns = 0;
            for (LayoutRowDefinition def : rows) {
                int current = def.getWidgets().length;
                if (current > columns) {
                    columns = current;
                }
            }
        }
        return columns;
    }

    public WidgetDefinition getWidgetDefinition(String name) {
        return widgets.get(name);
    }

    private static Map<String, Serializable> getProperties(
            Map<String, PropertiesDescriptor> map, String mode) {
        if (map == null) {
            return null;
        }
        PropertiesDescriptor defaultProps = map.get(BuiltinModes.ANY);
        PropertiesDescriptor props = map.get(mode);

        if (defaultProps == null && props == null) {
            return null;
        } else if (defaultProps == null) {
            return props.getProperties();
        } else if (props == null) {
            return defaultProps.getProperties();
        } else {
            // take any mode values, and override with given mode values
            Map<String, Serializable> res = new HashMap<String, Serializable>(
                    defaultProps.getProperties());
            res.putAll(props.getProperties());
            return res;
        }
    }

    public Map<String, Serializable> getProperties(String layoutMode) {
        Map<String, Serializable> modeProps = getProperties(properties,
                layoutMode);
        return modeProps;
    }

}
