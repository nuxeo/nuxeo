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
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;

/**
 * Default implementation for a widget definition.
 * <p>
 * Useful to compute widgets independently from the layout service.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class WidgetDefinitionImpl implements WidgetDefinition {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String type;

    protected Map<String, String> labels;

    protected Map<String, String> helpLabels;

    protected boolean translated = false;

    protected Map<String, String> modes;

    protected FieldDefinition[] fieldDefinitions;

    protected Map<String, Map<String, Serializable>> properties;

    protected Map<String, Map<String, Serializable>> widgetModeProperties;

    protected WidgetDefinition[] subWidgets;

    protected WidgetSelectOption[] selectOptions;

    // needed by GWT serialization
    protected WidgetDefinitionImpl() {
        super();
    }

    public WidgetDefinitionImpl(String name, String type, String label,
            String helpLabel, boolean translated, Map<String, String> modes,
            List<FieldDefinition> fieldDefinitions,
            Map<String, Serializable> properties,
            List<WidgetDefinition> subWidgets) {
        super();
        this.name = name;
        this.type = type;
        this.labels = new HashMap<String, String>();
        if (label != null) {
            this.labels.put(BuiltinModes.ANY, label);
        }
        this.helpLabels = new HashMap<String, String>();
        if (helpLabel != null) {
            this.helpLabels.put(BuiltinModes.ANY, helpLabel);
        }
        this.translated = translated;
        this.modes = modes;
        if (fieldDefinitions == null) {
            this.fieldDefinitions = new FieldDefinition[0];
        } else {
            this.fieldDefinitions = fieldDefinitions.toArray(new FieldDefinition[] {});
        }
        this.properties = new HashMap<String, Map<String, Serializable>>();
        if (properties != null) {
            this.properties.put(BuiltinModes.ANY, properties);
        }
        this.widgetModeProperties = null;
        if (subWidgets == null) {
            this.subWidgets = new WidgetDefinition[0];
        } else {
            this.subWidgets = subWidgets.toArray(new WidgetDefinition[] {});
        }
    }

    public WidgetDefinitionImpl(String name, String type,
            Map<String, String> labels, Map<String, String> helpLabels,
            boolean translated, Map<String, String> modes,
            List<FieldDefinition> fieldDefinitions,
            Map<String, Map<String, Serializable>> properties,
            Map<String, Map<String, Serializable>> widgetModeProperties,
            List<WidgetDefinition> subWidgets) {
        super();
        this.name = name;
        this.type = type;
        this.labels = labels;
        this.helpLabels = helpLabels;
        this.translated = translated;
        this.modes = modes;
        if (fieldDefinitions == null) {
            this.fieldDefinitions = new FieldDefinition[0];
        } else {
            this.fieldDefinitions = fieldDefinitions.toArray(new FieldDefinition[] {});
        }
        this.properties = properties;
        this.widgetModeProperties = widgetModeProperties;
        if (subWidgets == null) {
            this.subWidgets = new WidgetDefinition[0];
        } else {
            this.subWidgets = subWidgets.toArray(new WidgetDefinition[] {});
        }
    }

    public WidgetDefinitionImpl(String name, String type,
            Map<String, String> labels, Map<String, String> helpLabels,
            boolean translated, Map<String, String> modes,
            FieldDefinition[] fieldDefinitions,
            Map<String, Map<String, Serializable>> properties,
            Map<String, Map<String, Serializable>> widgetModeProperties,
            WidgetDefinition[] subWidgets) {
        super();
        this.name = name;
        this.type = type;
        this.labels = labels;
        this.helpLabels = helpLabels;
        this.translated = translated;
        this.modes = modes;
        this.fieldDefinitions = fieldDefinitions;
        this.properties = properties;
        this.widgetModeProperties = widgetModeProperties;
        this.subWidgets = subWidgets;
    }

    /**
     * @since 5.4.2
     */
    public WidgetDefinitionImpl(String name, String type,
            Map<String, String> labels, Map<String, String> helpLabels,
            boolean translated, Map<String, String> modes,
            FieldDefinition[] fieldDefinitions,
            Map<String, Map<String, Serializable>> properties,
            Map<String, Map<String, Serializable>> widgetModeProperties,
            WidgetDefinition[] subWidgets, WidgetSelectOption[] selectOptions) {
        this(name, type, labels, helpLabels, translated, modes,
                fieldDefinitions, properties, widgetModeProperties, subWidgets);
        this.selectOptions = selectOptions;
    }

    @Override
    public FieldDefinition[] getFieldDefinitions() {
        return fieldDefinitions;
    }

    @Override
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

    @Override
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

    @Override
    public String getMode(String layoutMode) {
        if (modes != null) {
            String mode = modes.get(layoutMode);
            if (mode == null) {
                mode = modes.get(BuiltinModes.ANY);
            }
            return mode;
        }
        return null;
    }

    @Override
    public Map<String, String> getModes() {
        return modes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
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
        return properties;
    }

    @Override
    public Map<String, Map<String, Serializable>> getWidgetModeProperties() {
        return widgetModeProperties;
    }

    @Override
    public String getRequired(String layoutMode, String mode) {
        String res = "false";
        Map<String, Serializable> props = getProperties(layoutMode, mode);
        if (props != null && props.containsKey(REQUIRED_PROPERTY_NAME)) {
            Object value = props.get(REQUIRED_PROPERTY_NAME);
            if (value instanceof Boolean) {
                res = ((Boolean) value).toString();
            } else if (value instanceof String) {
                res = (String) value;
            }
        }
        return res;
    }

    @Override
    public WidgetDefinition[] getSubWidgetDefinitions() {
        return subWidgets;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public boolean isTranslated() {
        return translated;
    }

    public static final Map<String, Serializable> getProperties(
            Map<String, Map<String, Serializable>> properties, String mode) {
        Map<String, Serializable> res = new HashMap<String, Serializable>();
        if (properties != null) {
            Map<String, Serializable> propsInMode = properties.get(mode);
            if (propsInMode != null) {
                res.putAll(propsInMode);
            }
            Map<String, Serializable> propsInAnyMode = properties.get(BuiltinModes.ANY);
            if (propsInAnyMode != null) {
                res.putAll(propsInAnyMode);
            }
        }
        return res;
    }

    @Override
    public WidgetSelectOption[] getSelectOptions() {
        return selectOptions;
    }

}
