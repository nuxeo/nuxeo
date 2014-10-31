/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
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
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.impl.LayoutTypeConfigurationImpl;
import org.w3c.dom.DocumentFragment;

/**
 * @since 6.0
 */
@XObject("configuration")
public class LayoutTypeConfigurationDescriptor {

    private static final Log log = LogFactory.getLog(LayoutTypeConfigurationDescriptor.class);

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

    @XNode("handlingLabels")
    boolean handlingLabels = false;

    /**
     * List of supported controls (controls checked on subwidgets
     * configuration).
     */
    @XNodeList(value = "supportedControls/control", type = ArrayList.class, componentType = String.class)
    List<String> supportedControls;

    @XNode("containingForm")
    boolean containingForm = false;

    @XNodeList(value = "categories/category", type = ArrayList.class, componentType = String.class)
    List<String> categories;

    @XNodeMap(value = "properties/layouts", key = "@mode", type = HashMap.class, componentType = LayoutDescriptors.class)
    Map<String, LayoutDescriptors> propertyLayouts;

    @XNodeMap(value = "properties/defaultValues", key = "@mode", type = HashMap.class, componentType = PropertiesDescriptor.class)
    Map<String, PropertiesDescriptor> defaultPropertyValues;

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

    public boolean isContainingForm() {
        return containingForm;
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

    public LayoutTypeConfiguration getLayoutTypeConfiguration() {
        LayoutTypeConfigurationImpl res = new LayoutTypeConfigurationImpl();
        res.setSinceVersion(getSinceVersion());
        res.setDeprecatedVersion(getDeprecatedVersion());
        res.setTitle(getTitle());
        res.setDescription(getDescription());
        res.setDemoId(getDemoId());
        res.setDemoPreviewEnabled(isDemoPreviewEnabled());
        res.setSupportedModes(getSupportedModes());
        res.setHandlingLabels(isHandlingLabels());
        res.setContainingForm(isContainingForm());
        res.setCategories(getCategories());
        res.setPropertyLayouts(getPropertyLayouts());
        res.setDefaultPropertyValues(getDefaultPropertyValues());
        res.setSupportedControls(getSupportedControls());
        return res;
    }

}
