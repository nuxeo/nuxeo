/*
 * (C) Copyright 2009-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;

public class WidgetTypeConfigurationImpl implements WidgetTypeConfiguration {

    private static final long serialVersionUID = 1L;

    protected String sinceVersion;

    protected String deprecatedVersion;

    protected String title;

    protected String description;

    protected String demoId;

    protected boolean demoPreviewEnabled = false;

    protected Map<String, Serializable> properties;

    protected List<String> supportedModes;

    protected boolean acceptingSubWidgets = false;

    protected boolean handlingLabels = false;

    /**
     * @since 5.9.1
     */
    protected List<String> supportedControls;

    protected boolean list = false;

    protected boolean complex = false;

    protected boolean containingForm = false;

    protected List<String> supportedFieldTypes;

    protected List<String> defaultFieldTypes;

    protected List<FieldDefinition> defaultFieldDefinitions;

    protected List<String> categories;

    protected Map<String, List<LayoutDefinition>> propertyLayouts;

    protected Map<String, Map<String, Serializable>> defaultPropertyValues;

    protected Map<String, Map<String, Serializable>> defaultControlValues;

    protected Map<String, List<LayoutDefinition>> fieldLayouts;

    public WidgetTypeConfigurationImpl() {
        super();
        this.properties = Collections.emptyMap();
        this.supportedModes = Collections.emptyList();
        this.supportedFieldTypes = Collections.emptyList();
        this.defaultFieldTypes = Collections.emptyList();
        this.defaultFieldDefinitions = Collections.emptyList();
        this.categories = Collections.emptyList();
        this.propertyLayouts = Collections.emptyMap();
    }

    @Override
    public String getSinceVersion() {
        return sinceVersion;
    }

    @Override
    public String getDeprecatedVersion() {
        return deprecatedVersion;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String getDemoId() {
        return demoId;
    }

    @Override
    public boolean isDemoPreviewEnabled() {
        return demoPreviewEnabled;
    }

    @Override
    public Map<String, Serializable> getConfProperties() {
        return properties;
    }

    @Override
    public Serializable getConfProperty(String propName) {
        if (properties == null) {
            return null;
        }
        return properties.get(propName);
    }

    @Override
    public List<String> getSupportedModes() {
        return supportedModes;
    }

    @Override
    public boolean isAcceptingSubWidgets() {
        return acceptingSubWidgets;
    }

    public boolean isList() {
        return list;
    }

    public boolean isComplex() {
        return complex;
    }

    public boolean isContainingForm() {
        return containingForm;
    }

    public List<String> getSupportedFieldTypes() {
        return supportedFieldTypes;
    }

    public List<String> getDefaultFieldTypes() {
        return defaultFieldTypes;
    }

    public List<FieldDefinition> getDefaultFieldDefinitions() {
        return defaultFieldDefinitions;
    }

    public List<String> getCategories() {
        return categories;
    }

    public Map<String, List<LayoutDefinition>> getPropertyLayouts() {
        return propertyLayouts;
    }

    public List<LayoutDefinition> getLayouts(Map<String, List<LayoutDefinition>> allLayouts, String mode,
            String additionalMode) {
        if (allLayouts != null) {
            List<LayoutDefinition> res = new ArrayList<>();
            if (additionalMode != null) {
                List<LayoutDefinition> defaultLayouts = allLayouts.get(additionalMode);
                if (defaultLayouts != null) {
                    res.addAll(defaultLayouts);
                }
            }
            List<LayoutDefinition> modeLayouts = allLayouts.get(mode);
            if (modeLayouts != null) {
                res.addAll(modeLayouts);
            }
            return res;
        }
        return null;
    }

    public List<LayoutDefinition> getPropertyLayouts(String mode, String additionalMode) {
        return getLayouts(propertyLayouts, mode, additionalMode);
    }

    /**
     * @since 5.6
     */
    public void setSinceVersion(String sinceVersion) {
        this.sinceVersion = sinceVersion;
    }

    /**
     * @since 5.6
     */
    public void setDeprecatedVersion(String deprecatedVersion) {
        this.deprecatedVersion = deprecatedVersion;
    }

    /**
     * @since 5.6
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @since 5.6
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @since 5.6
     */
    public void setDemoId(String demoId) {
        this.demoId = demoId;
    }

    /**
     * @since 5.6
     */
    public void setDemoPreviewEnabled(boolean demoPreviewEnabled) {
        this.demoPreviewEnabled = demoPreviewEnabled;
    }

    /**
     * @since 5.6
     */
    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

    /**
     * @since 5.6
     */
    public void setSupportedModes(List<String> supportedModes) {
        this.supportedModes = supportedModes;
    }

    /**
     * @since 5.6
     */
    public void setAcceptingSubWidgets(boolean acceptingSubWidgets) {
        this.acceptingSubWidgets = acceptingSubWidgets;
    }

    /**
     * @since 5.6
     */
    public void setList(boolean list) {
        this.list = list;
    }

    /**
     * @since 5.6
     */
    public void setComplex(boolean complex) {
        this.complex = complex;
    }

    /**
     * @since 5.6
     */
    public void setContainingForm(boolean containingForm) {
        this.containingForm = containingForm;
    }

    /**
     * @since 5.6
     */
    public void setSupportedFieldTypes(List<String> supportedFieldTypes) {
        this.supportedFieldTypes = supportedFieldTypes;
    }

    /**
     * @since 5.6
     */
    public void setDefaultFieldTypes(List<String> defaultFieldTypes) {
        this.defaultFieldTypes = defaultFieldTypes;
    }

    /**
     * @since 5.6
     */
    public void setDefaultFieldDefinitions(List<FieldDefinition> defaultFieldDefinitions) {
        this.defaultFieldDefinitions = defaultFieldDefinitions;
    }

    /**
     * @since 5.6
     */
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    /**
     * @since 5.6
     */
    public void setPropertyLayouts(Map<String, List<LayoutDefinition>> propertyLayouts) {
        this.propertyLayouts = propertyLayouts;
    }

    /**
     * @since 5.6
     */
    public boolean isHandlingLabels() {
        return handlingLabels;
    }

    /**
     * @since 5.6
     */
    public void setHandlingLabels(boolean handlingLabels) {
        this.handlingLabels = handlingLabels;
    }

    /**
     * @since 5.7.3
     */
    public Map<String, Map<String, Serializable>> getDefaultPropertyValues() {
        return defaultPropertyValues;
    }

    /**
     * @since 5.7.3
     */
    public Map<String, Serializable> getDefaultPropertyValues(String mode) {
        if (defaultPropertyValues != null) {
            Map<String, Serializable> res = new HashMap<>();
            Map<String, Serializable> anyProps = defaultPropertyValues.get(BuiltinModes.ANY);
            if (anyProps != null) {
                res.putAll(anyProps);
            }
            Map<String, Serializable> modeProps = defaultPropertyValues.get(mode);
            if (modeProps != null) {
                res.putAll(modeProps);
            }
            return res;
        }
        return null;
    }

    /**
     * @since 5.7.3
     */
    public void setDefaultPropertyValues(Map<String, Map<String, Serializable>> values) {
        this.defaultPropertyValues = values;
    }

    /**
     * @since 6.0
     */
    public Map<String, Map<String, Serializable>> getDefaultControlValues() {
        return defaultControlValues;
    }

    /**
     * @since 6.0
     */
    public Map<String, Serializable> getDefaultControlValues(String mode) {
        if (defaultControlValues != null) {
            Map<String, Serializable> res = new HashMap<>();
            Map<String, Serializable> anyProps = defaultControlValues.get(BuiltinModes.ANY);
            if (anyProps != null) {
                res.putAll(anyProps);
            }
            Map<String, Serializable> modeProps = defaultControlValues.get(mode);
            if (modeProps != null) {
                res.putAll(modeProps);
            }
            return res;
        }
        return null;
    }

    /**
     * @since 6.0
     */
    public void setDefaultControlValues(Map<String, Map<String, Serializable>> values) {
        this.defaultControlValues = values;
    }

    @Override
    public Map<String, List<LayoutDefinition>> getFieldLayouts() {
        return fieldLayouts;
    }

    @Override
    public List<LayoutDefinition> getFieldLayouts(String mode, String additionalMode) {
        return getLayouts(fieldLayouts, mode, additionalMode);
    }

    /**
     * @since 5.7.3
     */
    public void setFieldLayouts(Map<String, List<LayoutDefinition>> fieldLayouts) {
        this.fieldLayouts = fieldLayouts;
    }

    /**
     * @since 5.9.1
     */
    @Override
    public List<String> getSupportedControls() {
        return supportedControls;
    }

    /**
     * @since 5.9.1
     */
    public void setSupportedControls(List<String> supportedControls) {
        this.supportedControls = supportedControls;
    }

    /**
     * @since 7.2
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WidgetTypeConfigurationImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        WidgetTypeConfigurationImpl wc = (WidgetTypeConfigurationImpl) obj;
        return new EqualsBuilder().append(sinceVersion, wc.sinceVersion)
                                  .append(deprecatedVersion, wc.deprecatedVersion)
                                  .append(title, wc.title)
                                  .append(description, wc.description)
                                  .append(demoId, wc.demoId)
                                  .append(demoPreviewEnabled, wc.demoPreviewEnabled)
                                  .append(properties, wc.properties)
                                  .append(supportedModes, wc.supportedModes)
                                  .append(acceptingSubWidgets, wc.acceptingSubWidgets)
                                  .append(handlingLabels, wc.handlingLabels)
                                  .append(supportedControls, wc.supportedControls)
                                  .append(list, wc.list)
                                  .append(complex, wc.complex)
                                  .append(containingForm, wc.containingForm)
                                  .append(supportedFieldTypes, wc.supportedFieldTypes)
                                  .append(defaultFieldTypes, wc.defaultFieldTypes)
                                  .append(defaultFieldDefinitions, wc.defaultFieldDefinitions)
                                  .append(categories, wc.categories)
                                  .append(propertyLayouts, wc.propertyLayouts)
                                  .append(defaultPropertyValues, wc.defaultPropertyValues)
                                  .append(defaultControlValues, wc.defaultControlValues)
                                  .append(fieldLayouts, wc.fieldLayouts)
                                  .isEquals();
    }

}
