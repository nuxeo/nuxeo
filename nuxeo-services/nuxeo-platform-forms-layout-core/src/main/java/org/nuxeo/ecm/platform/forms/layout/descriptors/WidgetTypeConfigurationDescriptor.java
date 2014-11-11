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
public class WidgetTypeConfigurationDescriptor {

    private static final Log log = LogFactory.getLog(WidgetTypeConfigurationDescriptor.class);

    @XNode("sinceVersion")
    String sinceVersion;

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

    @XNode("fields/list")
    boolean list = false;

    @XNode("fields/complex")
    boolean complex = false;

    @XNodeList(value = "fields/supportedTypes/type", type = ArrayList.class, componentType = String.class)
    List<String> supportedFieldTypes;

    @XNodeList(value = "fields/defaultTypes/type", type = ArrayList.class, componentType = String.class)
    List<String> defaultFieldTypes;

    @XNodeList(value = "fields/defaultConfiguration/field", type = ArrayList.class, componentType = FieldDescriptor.class)
    List<FieldDescriptor> defaultFieldDefinitions;

    @XNodeList(value = "categories/category", type = ArrayList.class, componentType = String.class)
    List<String> categories;

    @XNodeMap(value = "properties/layouts", key = "@mode", type = HashMap.class, componentType = LayoutDescriptors.class)
    Map<String, LayoutDescriptors> propertyLayouts;

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

    public boolean isComplex() {
        return complex;
    }

    public boolean isList() {
        return list;
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

    public List<LayoutDefinition> getPropertyLayouts(String mode,
            String additionalMode) {
        if (propertyLayouts != null) {
            List<LayoutDefinition> res = new ArrayList<LayoutDefinition>();
            if (additionalMode != null) {
                LayoutDescriptors defaultLayouts = propertyLayouts.get(additionalMode);
                if (defaultLayouts != null) {
                    List<LayoutDefinition> defaultLayoutsList = defaultLayouts.getLayouts();
                    if (defaultLayoutsList != null) {
                        res.addAll(defaultLayoutsList);
                    }
                }
            }
            LayoutDescriptors modeLayouts = propertyLayouts.get(mode);
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

    public Map<String, List<LayoutDefinition>> getPropertyLayouts() {
        if (propertyLayouts != null) {
            Map<String, List<LayoutDefinition>> res = new HashMap<String, List<LayoutDefinition>>();
            for (Map.Entry<String, LayoutDescriptors> entry : propertyLayouts.entrySet()) {
                res.put(entry.getKey(), entry.getValue().getLayouts());
            }
            return res;
        }
        return null;
    }

    public String getSinceVersion() {
        return sinceVersion;
    }

    public List<String> getSupportedModes() {
        return supportedModes;
    }

    public WidgetTypeConfiguration getWidgetTypeConfiguration() {
        return new WidgetTypeConfigurationImpl(getSinceVersion(), getTitle(),
                getDescription(), getDemoId(), isDemoPreviewEnabled(),
                getConfProperties(), getSupportedModes(),
                isAcceptingSubWidgets(), isList(), isComplex(),
                getSupportedFieldTypes(), getDefaultFieldTypes(),
                getDefaultFieldDefinitions(), getCategories(),
                getPropertyLayouts());
    }
}
