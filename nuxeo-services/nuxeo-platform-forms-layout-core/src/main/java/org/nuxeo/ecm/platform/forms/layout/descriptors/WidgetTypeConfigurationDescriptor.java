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
package org.nuxeo.ecm.platform.forms.layout.descriptors;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.serialize.OutputFormat;
import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.impl.WidgetTypeConfigurationImpl;
import org.w3c.dom.DocumentFragment;

/**
 * Descriptor for a widget type configuration
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("configuration")
@SuppressWarnings("deprecation")
public class WidgetTypeConfigurationDescriptor {

    private static final Log log = LogFactory.getLog(WidgetTypeConfigurationDescriptor.class);

    @XNode("sinceVersion")
    String sinceVersion;

    /**
     * @since 5.6
     */
    @XNode("deprecatedVersion")
    String deprecatedVersion;

    @XNode("title")
    String title;

    // retrieve HTML tags => introspect DOM on setter
    String description;

    @XNode("demo@id")
    String demoId;

    @XNode("demo@previewEnabled")
    boolean demoPreviewEnabled = false;

    @XNodeList(value = "supportedModes/mode", type = ArrayList.class, componentType = String.class)
    List<String> supportedModes;

    @XNode("acceptingSubWidgets")
    boolean acceptingSubWidgets = false;

    /**
     * @since 5.6
     */
    @XNode("handlingLabels")
    boolean handlingLabels = false;

    /**
     * List of supported controls (controls checked on subwidgets
     * configuration).
     *
     * @since 5.9.1
     */
    @XNodeList(value = "supportedControls/control", type = ArrayList.class, componentType = String.class)
    List<String> supportedControls;

    @XNode("fields/list")
    boolean list = false;

    @XNode("fields/complex")
    boolean complex = false;

    @XNode("containingForm")
    boolean containingForm = false;

    @XNodeList(value = "fields/supportedTypes/type", type = ArrayList.class, componentType = String.class)
    List<String> supportedFieldTypes;

    @XNodeList(value = "fields/defaultTypes/type", type = ArrayList.class, componentType = String.class)
    List<String> defaultFieldTypes;

    @XNodeList(value = "fields/defaultConfiguration/field", type = ArrayList.class, componentType = FieldDescriptor.class)
    List<FieldDescriptor> defaultFieldDefinitions;

    /**
     * Layouts for accepted field mappings.
     *
     * @since 5.7.3
     */
    @XNodeMap(value = "fields/layouts", key = "@mode", type = HashMap.class, componentType = LayoutDescriptors.class)
    Map<String, LayoutDescriptors> fieldLayouts;

    @XNodeList(value = "categories/category", type = ArrayList.class, componentType = String.class)
    List<String> categories;

    @XNodeMap(value = "properties/layouts", key = "@mode", type = HashMap.class, componentType = LayoutDescriptors.class)
    Map<String, LayoutDescriptors> propertyLayouts;

    /**
     * @since 5.7.3
     */
    @XNodeMap(value = "properties/defaultValues", key = "@mode", type = HashMap.class, componentType = PropertiesDescriptor.class)
    Map<String, PropertiesDescriptor> defaultPropertyValues;

    /**
     * @since 5.7.3
     */
    @XNodeMap(value = "controls/defaultValues", key = "@mode", type = HashMap.class, componentType = ControlsDescriptor.class)
    Map<String, ControlsDescriptor> defaultControlValues;

    Map<String, Serializable> properties;

    public List<String> getCategories() {
        return categories;
    }

    public String getDescription() {
        return description;
    }

    @XContent("description")
    public void setDescription(DocumentFragment descriptionDOM) {
        try {
            OutputFormat of = new OutputFormat();
            of.setOmitXMLDeclaration(true);
            this.description = DOMSerializer.toString(descriptionDOM, of).trim();
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getDemoId() {
        return demoId;
    }

    public boolean isDemoPreviewEnabled() {
        return demoPreviewEnabled;
    }

    public boolean isAcceptingSubWidgets() {
        return acceptingSubWidgets;
    }

    /**
     * @since 5.6
     */
    public boolean isHandlingLabels() {
        return handlingLabels;
    }

    /**
     * @since 5.9.1
     */
    public List<String> getSupportedControls() {
        return supportedControls;
    }

    public boolean isComplex() {
        return complex;
    }

    public boolean isList() {
        return list;
    }

    public boolean isContainingForm() {
        return containingForm;
    }

    public List<String> getDefaultFieldTypes() {
        return defaultFieldTypes;
    }

    public List<String> getSupportedFieldTypes() {
        return supportedFieldTypes;
    }

    public List<FieldDefinition> getDefaultFieldDefinitions() {
        if (defaultFieldDefinitions == null) {
            return null;
        }
        List<FieldDefinition> res = new ArrayList<FieldDefinition>();
        for (int i = 0; i < defaultFieldDefinitions.size(); i++) {
            res.add(defaultFieldDefinitions.get(i).getFieldDefinition());
        }
        return res;
    }

    public Map<String, Serializable> getConfProperties() {
        return properties;
    }

    public Serializable getConfProperty(String propName) {
        if (properties == null) {
            return null;
        }
        return properties.get(propName);
    }

    @XNode("confProperties")
    public void setConfProperties(PropertiesDescriptor propsDesc) {
        properties = propsDesc.getProperties();
    }

    protected List<LayoutDefinition> getLayouts(
            Map<String, LayoutDescriptors> descs, String mode,
            String additionalMode) {
        if (descs != null) {
            List<LayoutDefinition> res = new ArrayList<LayoutDefinition>();
            if (additionalMode != null) {
                LayoutDescriptors defaultLayouts = descs.get(additionalMode);
                if (defaultLayouts != null) {
                    List<LayoutDefinition> defaultLayoutsList = defaultLayouts.getLayouts();
                    if (defaultLayoutsList != null) {
                        res.addAll(defaultLayoutsList);
                    }
                }
            }
            LayoutDescriptors modeLayouts = descs.get(mode);
            if (modeLayouts != null) {
                List<LayoutDefinition> modeLayoutsList = modeLayouts.getLayouts();
                if (modeLayoutsList != null) {
                    res.addAll(modeLayoutsList);
                }
            }
            return res;
        }
        return null;
    }

    protected Map<String, List<LayoutDefinition>> getLayouts(
            Map<String, LayoutDescriptors> descs) {
        if (descs != null) {
            Map<String, List<LayoutDefinition>> res = new HashMap<String, List<LayoutDefinition>>();
            for (Map.Entry<String, LayoutDescriptors> entry : descs.entrySet()) {
                res.put(entry.getKey(), entry.getValue().getLayouts());
            }
            return res;
        }
        return null;
    }

    public List<LayoutDefinition> getPropertyLayouts(String mode,
            String additionalMode) {
        return getLayouts(propertyLayouts, mode, additionalMode);
    }

    public Map<String, List<LayoutDefinition>> getPropertyLayouts() {
        return getLayouts(propertyLayouts);
    }

    public List<LayoutDefinition> getFieldLayouts(String mode,
            String additionalMode) {
        return getLayouts(fieldLayouts, mode, additionalMode);
    }

    public Map<String, List<LayoutDefinition>> getFieldLayouts() {
        return getLayouts(fieldLayouts);
    }

    public Map<String, Map<String, Serializable>> getDefaultPropertyValues() {
        if (defaultPropertyValues != null) {
            Map<String, Map<String, Serializable>> res = new HashMap<String, Map<String, Serializable>>();
            for (Map.Entry<String, PropertiesDescriptor> entry : defaultPropertyValues.entrySet()) {
                res.put(entry.getKey(), entry.getValue().getProperties());
            }
            return res;
        }
        return null;
    }

    public Map<String, Map<String, Serializable>> getDefaultControlValues() {
        if (defaultControlValues != null) {
            Map<String, Map<String, Serializable>> res = new HashMap<String, Map<String, Serializable>>();
            for (Map.Entry<String, ControlsDescriptor> entry : defaultControlValues.entrySet()) {
                res.put(entry.getKey(), entry.getValue().getControls());
            }
            return res;
        }
        return null;
    }

    public String getSinceVersion() {
        return sinceVersion;
    }

    /**
     * @since 5.6
     */
    public String getDeprecatedVersion() {
        return deprecatedVersion;
    }

    public List<String> getSupportedModes() {
        return supportedModes;
    }

    public WidgetTypeConfiguration getWidgetTypeConfiguration() {
        WidgetTypeConfigurationImpl res = new WidgetTypeConfigurationImpl();
        res.setSinceVersion(getSinceVersion());
        res.setDeprecatedVersion(getDeprecatedVersion());
        res.setTitle(getTitle());
        res.setDescription(getDescription());
        res.setDemoId(getDemoId());
        res.setDemoPreviewEnabled(isDemoPreviewEnabled());
        res.setProperties(getConfProperties());
        res.setSupportedModes(getSupportedModes());
        res.setAcceptingSubWidgets(isAcceptingSubWidgets());
        res.setHandlingLabels(isHandlingLabels());
        res.setList(isList());
        res.setComplex(isComplex());
        res.setContainingForm(isContainingForm());
        res.setSupportedFieldTypes(getSupportedFieldTypes());
        res.setDefaultFieldTypes(getDefaultFieldTypes());
        res.setDefaultFieldDefinitions(getDefaultFieldDefinitions());
        res.setCategories(getCategories());
        res.setPropertyLayouts(getPropertyLayouts());
        res.setDefaultPropertyValues(getDefaultPropertyValues());
        res.setDefaultControlValues(getDefaultControlValues());
        res.setFieldLayouts(getFieldLayouts());
        res.setSupportedControls(getSupportedControls());
        return res;
    }
}
