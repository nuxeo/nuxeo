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
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.RenderingInfo;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
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

    protected String typeCategory;

    protected Map<String, String> labels;

    protected Map<String, String> helpLabels;

    protected boolean translated = false;

    /**
     * @deprecated since 5.7: use {@link #controls} instead
     */
    @Deprecated
    protected boolean handlingLabels = false;

    protected Map<String, String> modes;

    protected FieldDefinition[] fieldDefinitions;

    protected Map<String, Map<String, Serializable>> properties;

    protected Map<String, Map<String, Serializable>> widgetModeProperties;

    protected Map<String, Map<String, Serializable>> controls;

    protected WidgetDefinition[] subWidgets;

    protected WidgetReference[] subWidgetReferences;

    protected WidgetSelectOption[] selectOptions;

    protected Map<String, List<RenderingInfo>> renderingInfos;

    protected List<String> aliases;

    protected boolean dynamic = false;

    protected boolean global = false;

    // needed by GWT serialization
    protected WidgetDefinitionImpl() {
        super();
    }

    public WidgetDefinitionImpl(String name, String type, String label, String helpLabel, boolean translated,
            Map<String, String> modes, List<FieldDefinition> fieldDefinitions, Map<String, Serializable> properties,
            List<WidgetDefinition> subWidgets) {
        super();
        this.name = name;
        this.type = type;
        this.labels = new HashMap<>();
        if (label != null) {
            this.labels.put(BuiltinModes.ANY, label);
        }
        this.helpLabels = new HashMap<>();
        if (helpLabel != null) {
            this.helpLabels.put(BuiltinModes.ANY, helpLabel);
        }
        this.translated = translated;
        this.modes = modes;
        if (fieldDefinitions == null) {
            this.fieldDefinitions = new FieldDefinition[0];
        } else {
            this.fieldDefinitions = fieldDefinitions.toArray(new FieldDefinition[0]);
        }
        this.properties = new HashMap<>();
        if (properties != null) {
            this.properties.put(BuiltinModes.ANY, properties);
        }
        this.widgetModeProperties = null;
        if (subWidgets == null) {
            this.subWidgets = new WidgetDefinition[0];
        } else {
            this.subWidgets = subWidgets.toArray(new WidgetDefinition[0]);
        }
    }

    public WidgetDefinitionImpl(String name, String type, Map<String, String> labels, Map<String, String> helpLabels,
            boolean translated, Map<String, String> modes, List<FieldDefinition> fieldDefinitions,
            Map<String, Map<String, Serializable>> properties,
            Map<String, Map<String, Serializable>> widgetModeProperties, List<WidgetDefinition> subWidgets) {
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
            this.fieldDefinitions = fieldDefinitions.toArray(new FieldDefinition[0]);
        }
        this.properties = properties;
        this.widgetModeProperties = widgetModeProperties;
        if (subWidgets == null) {
            this.subWidgets = new WidgetDefinition[0];
        } else {
            this.subWidgets = subWidgets.toArray(new WidgetDefinition[0]);
        }
    }

    public WidgetDefinitionImpl(String name, String type, Map<String, String> labels, Map<String, String> helpLabels,
            boolean translated, Map<String, String> modes, FieldDefinition[] fieldDefinitions,
            Map<String, Map<String, Serializable>> properties,
            Map<String, Map<String, Serializable>> widgetModeProperties, WidgetDefinition[] subWidgets) {
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
    public WidgetDefinitionImpl(String name, String type, Map<String, String> labels, Map<String, String> helpLabels,
            boolean translated, Map<String, String> modes, FieldDefinition[] fieldDefinitions,
            Map<String, Map<String, Serializable>> properties,
            Map<String, Map<String, Serializable>> widgetModeProperties, WidgetDefinition[] subWidgets,
            WidgetSelectOption[] selectOptions) {
        this(name, type, labels, helpLabels, translated, modes, fieldDefinitions, properties, widgetModeProperties,
                subWidgets);
        this.selectOptions = selectOptions;
    }

    @Override
    public FieldDefinition[] getFieldDefinitions() {
        return fieldDefinitions;
    }

    @Override
    public void setFieldDefinitions(FieldDefinition[] fieldDefinitions) {
        this.fieldDefinitions = fieldDefinitions;
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
    public void setHelpLabels(Map<String, String> helpLabels) {
        this.helpLabels = helpLabels;
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
    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
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
    public void setModes(Map<String, String> modes) {
        this.modes = modes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Map<String, Serializable> getProperties(String layoutMode, String mode) {
        Map<String, Serializable> modeProps = getProperties(properties, layoutMode);
        Map<String, Serializable> widgetModeProps = getProperties(widgetModeProperties, mode);
        // take mode values, and override with widget mode values
        Map<String, Serializable> res = new HashMap<>(modeProps);
        res.putAll(widgetModeProps);
        return res;
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
    public Map<String, Map<String, Serializable>> getWidgetModeProperties() {
        return widgetModeProperties;
    }

    @Override
    public void setWidgetModeProperties(Map<String, Map<String, Serializable>> widgetModeProperties) {
        this.widgetModeProperties = widgetModeProperties;
    }

    @Override
    public Map<String, Serializable> getControls(String layoutMode, String mode) {
        return getProperties(controls, layoutMode);
    }

    @Override
    public Map<String, Map<String, Serializable>> getControls() {
        return controls;
    }

    @Override
    public void setControls(Map<String, Map<String, Serializable>> controls) {
        this.controls = controls;
    }

    @Override
    public String getRequired(String layoutMode, String mode) {
        String res = "false";
        Map<String, Serializable> props = getProperties(layoutMode, mode);
        if (props != null && props.containsKey(REQUIRED_PROPERTY_NAME)) {
            Object value = props.get(REQUIRED_PROPERTY_NAME);
            if (value instanceof Boolean) {
                res = value.toString();
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
    public void setSubWidgetDefinitions(WidgetDefinition[] subWidgets) {
        this.subWidgets = subWidgets;
    }

    @Override
    public WidgetReference[] getSubWidgetReferences() {
        return subWidgetReferences;
    }

    @Override
    public void setSubWidgetReferences(WidgetReference[] subWidgetReferences) {
        this.subWidgetReferences = subWidgetReferences;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getTypeCategory() {
        return typeCategory;
    }

    @Override
    public void setTypeCategory(String typeCategory) {
        this.typeCategory = typeCategory;
    }

    @Override
    public boolean isTranslated() {
        return translated;
    }

    @Override
    public void setTranslated(boolean translated) {
        this.translated = translated;
    }

    @Override
    public boolean isHandlingLabels() {
        // migration code
        Map<String, Serializable> controls = getControls(BuiltinModes.ANY, BuiltinModes.ANY);
        if (controls != null && controls.containsKey("handleLabels")) {
            Serializable handling = controls.get("handleLabels");
            if (handling != null) {
                return Boolean.parseBoolean(handling.toString());
            }
        }
        return handlingLabels;
    }

    @Override
    public void setHandlingLabels(boolean handlingLabels) {
        this.handlingLabels = handlingLabels;
    }

    public static Map<String, Serializable> getProperties(Map<String, Map<String, Serializable>> properties, String mode) {
        Map<String, Serializable> res = new HashMap<>();
        if (properties != null) {
            Map<String, Serializable> propsInAnyMode = properties.get(BuiltinModes.ANY);
            if (propsInAnyMode != null) {
                res.putAll(propsInAnyMode);
            }
            Map<String, Serializable> propsInMode = properties.get(mode);
            if (propsInMode != null) {
                res.putAll(propsInMode);
            }
        }
        return res;
    }

    @Override
    public WidgetSelectOption[] getSelectOptions() {
        return selectOptions;
    }

    @Override
    public void setSelectOptions(WidgetSelectOption[] selectOptions) {
        this.selectOptions = selectOptions;
    }

    @Override
    public Map<String, List<RenderingInfo>> getRenderingInfos() {
        return renderingInfos;
    }

    @Override
    public void setRenderingInfos(Map<String, List<RenderingInfo>> renderingInfos) {
        this.renderingInfos = renderingInfos;
    }

    public static List<RenderingInfo> getRenderingInfos(Map<String, List<RenderingInfo>> infos, String mode) {
        List<RenderingInfo> res = new ArrayList<>();
        if (infos != null) {
            List<RenderingInfo> inAnyMode = infos.get(BuiltinModes.ANY);
            if (inAnyMode != null) {
                res.addAll(inAnyMode);
            }
            List<RenderingInfo> inMode = infos.get(mode);
            if (inMode != null) {
                res.addAll(inMode);
            }
        }
        return res;
    }

    @Override
    public List<RenderingInfo> getRenderingInfos(String mode) {
        return getRenderingInfos(renderingInfos, mode);
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    @Override
    public boolean isGlobal() {
        return global;
    }

    @Override
    public void setGlobal(boolean global) {
        this.global = global;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    @Override
    public WidgetDefinition clone() {
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
        Map<String, Map<String, Serializable>> ccontrols = null;
        if (controls != null) {
            ccontrols = new HashMap<>();
            for (Map.Entry<String, Map<String, Serializable>> entry : controls.entrySet()) {
                Map<String, Serializable> subControls = entry.getValue();
                Map<String, Serializable> csubControls = null;
                if (subControls != null) {
                    csubControls = new HashMap<>();
                    csubControls.putAll(subControls);
                }
                ccontrols.put(entry.getKey(), csubControls);
            }
        }
        Map<String, String> clabels = null;
        if (labels != null) {
            clabels = new HashMap<>();
            clabels.putAll(labels);
        }
        Map<String, String> chelpLabels = null;
        if (helpLabels != null) {
            chelpLabels = new HashMap<>();
            chelpLabels.putAll(helpLabels);
        }
        Map<String, String> cmodes = null;
        if (modes != null) {
            cmodes = new HashMap<>();
            cmodes.putAll(modes);
        }
        FieldDefinition[] cfieldDefinitions = null;
        if (fieldDefinitions != null) {
            cfieldDefinitions = new FieldDefinition[fieldDefinitions.length];
            for (int i = 0; i < fieldDefinitions.length; i++) {
                cfieldDefinitions[i] = fieldDefinitions[i].clone();
            }
        }
        Map<String, Map<String, Serializable>> cwidgetProps = null;
        if (widgetModeProperties != null) {
            cwidgetProps = new HashMap<>();
            for (Map.Entry<String, Map<String, Serializable>> entry : widgetModeProperties.entrySet()) {
                Map<String, Serializable> subProps = entry.getValue();
                Map<String, Serializable> csubProps = null;
                if (subProps != null) {
                    csubProps = new HashMap<>();
                    csubProps.putAll(subProps);
                }
                cwidgetProps.put(entry.getKey(), csubProps);
            }
        }
        WidgetDefinition[] csubWidgets = null;
        if (subWidgets != null) {
            csubWidgets = new WidgetDefinition[subWidgets.length];
            for (int i = 0; i < subWidgets.length; i++) {
                csubWidgets[i] = subWidgets[i].clone();
            }
        }
        WidgetReference[] csubWidgetRefs = null;
        if (subWidgetReferences != null) {
            csubWidgetRefs = new WidgetReference[subWidgetReferences.length];
            for (int i = 0; i < subWidgetReferences.length; i++) {
                csubWidgetRefs[i] = subWidgetReferences[i].clone();
            }
        }
        WidgetSelectOption[] cselectOptions = null;
        if (selectOptions != null) {
            cselectOptions = new WidgetSelectOption[selectOptions.length];
            for (int i = 0; i < selectOptions.length; i++) {
                cselectOptions[i] = selectOptions[i].clone();
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
        WidgetDefinitionImpl clone = new WidgetDefinitionImpl(name, type, clabels, chelpLabels, translated, cmodes,
                cfieldDefinitions, cprops, cwidgetProps, csubWidgets, cselectOptions);
        clone.setTypeCategory(typeCategory);
        clone.setRenderingInfos(crenderingInfos);
        clone.setSubWidgetReferences(csubWidgetRefs);
        clone.setHandlingLabels(handlingLabels);
        clone.setControls(ccontrols);
        if (aliases != null) {
            clone.setAliases(new ArrayList<>(aliases));
        }
        clone.setDynamic(dynamic);
        clone.setGlobal(global);
        return clone;
    }

    /**
     * @since 7.2
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WidgetDefinitionImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        WidgetDefinitionImpl w = (WidgetDefinitionImpl) obj;
        return new EqualsBuilder().append(name, w.name).append(type, w.type).append(typeCategory, w.typeCategory).append(
                labels, w.labels).append(helpLabels, w.helpLabels).append(translated, w.translated).append(
                handlingLabels, w.handlingLabels).append(modes, w.modes).append(fieldDefinitions, w.fieldDefinitions).append(
                properties, w.properties).append(widgetModeProperties, w.widgetModeProperties).append(controls,
                w.controls).append(subWidgets, w.subWidgets).append(subWidgetReferences, w.subWidgetReferences).append(
                selectOptions, w.selectOptions).append(renderingInfos, w.renderingInfos).append(aliases, w.aliases).append(
                dynamic, w.dynamic).append(global, w.global).isEquals();
    }

    /**
     * @since 7.1
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("WidgetDefinitionImpl");
        sb.append(" {");
        sb.append(" name=");
        sb.append(name);
        sb.append(", type=");
        sb.append(type);
        sb.append(", typeCategory=");
        sb.append(typeCategory);
        sb.append(", properties=");
        sb.append(properties);
        sb.append(", controls=");
        sb.append(controls);
        sb.append('}');

        return sb.toString();
    }

}
