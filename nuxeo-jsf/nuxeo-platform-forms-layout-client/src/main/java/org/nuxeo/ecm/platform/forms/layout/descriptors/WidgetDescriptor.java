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
 * $Id: WidgetDescriptor.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Widget definition descriptor.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
@XObject("widget")
public class WidgetDescriptor implements WidgetDefinition {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(WidgetDescriptor.class);

    @XNode("@name")
    String name;

    @XNode("@type")
    String type;

    @XNodeList(value = "fields/field", type = FieldDescriptor[].class, componentType = FieldDescriptor.class)
    FieldDefinition[] fields = new FieldDefinition[0];

    @XNodeMap(value = "widgetModes/mode", key = "@value", type = HashMap.class, componentType = String.class)
    Map<String, String> modes = new HashMap<String, String>();

    @XNodeMap(value = "labels/label", key = "@mode", type = HashMap.class, componentType = String.class)
    Map<String, String> labels = new HashMap<String, String>();

    @XNodeMap(value = "helpLabels/label", key = "@mode", type = HashMap.class, componentType = String.class)
    Map<String, String> helpLabels = new HashMap<String, String>();

    @XNode("translated")
    boolean translated = true;

    @XNodeMap(value = "properties", key = "@mode", type = HashMap.class, componentType = PropertiesDescriptor.class)
    Map<String, PropertiesDescriptor> properties = new HashMap<String, PropertiesDescriptor>();

    @XNodeMap(value = "properties", key = "@widgetMode", type = HashMap.class, componentType = PropertiesDescriptor.class)
    Map<String, PropertiesDescriptor> widgetModeProperties = new HashMap<String, PropertiesDescriptor>();

    @XNodeList(value = "subWidgets/widget", type = WidgetDescriptor[].class, componentType = WidgetDescriptor.class)
    WidgetDefinition[] subWidgets = new WidgetDefinition[0];

    // set in method to mix single and multiple options
    WidgetSelectOption[] selectOptions = new WidgetSelectOption[0];

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public FieldDefinition[] getFieldDefinitions() {
        return fields;
    }

    public String getMode(String layoutMode) {
        String mode = modes.get(layoutMode);
        if (mode == null) {
            mode = modes.get(BuiltinModes.ANY);
        }
        return mode;
    }

    @Override
    public Map<String, String> getModes() {
        return modes;
    }

    public String getRequired(String layoutMode, String mode) {
        String res = "false";
        Map<String, Serializable> props = getProperties(layoutMode, mode);
        if (props != null && props.containsKey(REQUIRED_PROPERTY_NAME)) {
            Object value = props.get(REQUIRED_PROPERTY_NAME);
            if (value instanceof String) {
                res = (String) value;
            } else {
                log.error(String.format(
                        "Invalid property \"%s\" on widget %s: %s",
                        REQUIRED_PROPERTY_NAME, value, name));
            }
        }
        return res;
    }

    public String getLabel(String mode) {
        String label = labels.get(mode);
        if (label == null) {
            label = labels.get(BuiltinModes.ANY);
        }
        return label;
    }

    @Override
    public Map<String, String> getLabels() {
        return labels;
    }

    public String getHelpLabel(String mode) {
        String label = helpLabels.get(mode);
        if (label == null) {
            label = helpLabels.get(BuiltinModes.ANY);
        }
        return label;
    }

    @Override
    public Map<String, String> getHelpLabels() {
        return helpLabels;
    }

    public boolean isTranslated() {
        return translated;
    }

    public Map<String, Serializable> getProperties(String layoutMode,
            String mode) {
        Map<String, Serializable> modeProps = getProperties(properties,
                layoutMode);
        Map<String, Serializable> widgetModeProps = getProperties(
                widgetModeProperties, mode);
        if (modeProps == null && widgetModeProps == null) {
            return null;
        } else if (widgetModeProps == null) {
            return modeProps;
        } else if (modeProps == null) {
            return widgetModeProps;
        } else {
            // take mode values, and override with widget mode values
            Map<String, Serializable> res = new HashMap<String, Serializable>(
                    modeProps);
            res.putAll(widgetModeProps);
            return res;
        }
    }

    @Override
    public Map<String, Map<String, Serializable>> getProperties() {
        return getProperties(properties);
    }

    @Override
    public Map<String, Map<String, Serializable>> getWidgetModeProperties() {
        return getProperties(widgetModeProperties);
    }

    public WidgetDefinition[] getSubWidgetDefinitions() {
        return subWidgets;
    }

    public static Map<String, Serializable> getProperties(
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

    public static Map<String, Map<String, Serializable>> getProperties(
            Map<String, PropertiesDescriptor> map) {
        if (map == null) {
            return null;
        }
        Map<String, Map<String, Serializable>> res = new HashMap<String, Map<String, Serializable>>();
        for (Map.Entry<String, PropertiesDescriptor> item : map.entrySet()) {
            Map<String, Serializable> props = new HashMap<String, Serializable>();
            props.putAll(item.getValue().getProperties());
            res.put(item.getKey(), props);
        }
        return res;
    }

    public WidgetSelectOption[] getSelectOptions() {
        return selectOptions;
    }

    @XContent("selectOptions")
    public void setSelectOptions(DocumentFragment selectOptionsDOM) {
        XMap xmap = new XMap();
        xmap.register(WidgetSelectOptionDescriptor.class);
        xmap.register(WidgetSelectOptionsDescriptor.class);
        Node p = selectOptionsDOM.getFirstChild();
        List<WidgetSelectOption> options = new ArrayList<WidgetSelectOption>();
        while (p != null) {
            if (p.getNodeType() == Node.ELEMENT_NODE) {
                try {
                    options.add((WidgetSelectOption) xmap.load((Element) p));
                } catch (Exception e) {
                    log.error(e, e);
                }
            }
            p = p.getNextSibling();
        }
        selectOptions = options.toArray(new WidgetSelectOption[] {});
    }
}
