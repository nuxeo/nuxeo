/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
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

    @SuppressWarnings({ "unchecked" })
    public WidgetTypeConfigurationImpl() {
        this(null, null, null, null, false, Collections.EMPTY_MAP,
                Collections.EMPTY_LIST, false, false, false, false,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                Collections.EMPTY_MAP);
    }

    /**
     * @deprecated since 5.6: use setters instead
     */
    @Deprecated
    public WidgetTypeConfigurationImpl(String sinceVersion, String title,
            String description, String demoId, boolean demoPreviewEnabled,
            Map<String, Serializable> properties, List<String> supportedModes,
            boolean acceptingSubWidgets, boolean list, boolean complex,
            List<String> supportedFieldTypes, List<String> defaultFieldTypes,
            List<FieldDefinition> defaultFieldDefinitions,
            List<String> categories,
            Map<String, List<LayoutDefinition>> propertyLayouts) {
        this(sinceVersion, title, description, demoId, demoPreviewEnabled,
                properties, supportedModes, acceptingSubWidgets, list, complex,
                false, supportedFieldTypes, defaultFieldTypes,
                defaultFieldDefinitions, categories, propertyLayouts);
    }

    /**
     * @deprecated since 5.6: use setters instead
     */
    @Deprecated
    public WidgetTypeConfigurationImpl(String sinceVersion, String title,
            String description, String demoId, boolean demoPreviewEnabled,
            Map<String, Serializable> properties, List<String> supportedModes,
            boolean acceptingSubWidgets, boolean list, boolean complex,
            boolean containingForm, List<String> supportedFieldTypes,
            List<String> defaultFieldTypes,
            List<FieldDefinition> defaultFieldDefinitions,
            List<String> categories,
            Map<String, List<LayoutDefinition>> propertyLayouts) {
        super();
        this.sinceVersion = sinceVersion;
        this.title = title;
        this.description = description;
        this.demoId = demoId;
        this.demoPreviewEnabled = demoPreviewEnabled;
        this.properties = properties;
        this.supportedModes = supportedModes;
        this.acceptingSubWidgets = acceptingSubWidgets;
        this.list = list;
        this.complex = complex;
        this.containingForm = containingForm;
        this.supportedFieldTypes = supportedFieldTypes;
        this.defaultFieldTypes = defaultFieldTypes;
        this.defaultFieldDefinitions = defaultFieldDefinitions;
        this.categories = categories;
        this.propertyLayouts = propertyLayouts;
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

    public List<LayoutDefinition> getLayouts(
            Map<String, List<LayoutDefinition>> allLayouts, String mode,
            String additionalMode) {
        if (allLayouts != null) {
            List<LayoutDefinition> res = new ArrayList<LayoutDefinition>();
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

    public List<LayoutDefinition> getPropertyLayouts(String mode,
            String additionalMode) {
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
    public void setDefaultFieldDefinitions(
            List<FieldDefinition> defaultFieldDefinitions) {
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
    public void setPropertyLayouts(
            Map<String, List<LayoutDefinition>> propertyLayouts) {
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
            Map<String, Serializable> res = new HashMap<String, Serializable>();
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
    public void setDefaultPropertyValues(
            Map<String, Map<String, Serializable>> values) {
        this.defaultPropertyValues = values;
    }

    /**
     * @since 5.9.6
     */
    public Map<String, Map<String, Serializable>> getDefaultControlValues() {
        return defaultControlValues;
    }

    /**
     * @since 5.9.6
     */
    public Map<String, Serializable> getDefaultControlValues(String mode) {
        if (defaultControlValues != null) {
            Map<String, Serializable> res = new HashMap<String, Serializable>();
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
     * @since 5.9.6
     */
    public void setDefaultControlValues(
            Map<String, Map<String, Serializable>> values) {
        this.defaultControlValues = values;
    }

    @Override
    public Map<String, List<LayoutDefinition>> getFieldLayouts() {
        return fieldLayouts;
    }

    @Override
    public List<LayoutDefinition> getFieldLayouts(String mode,
            String additionalMode) {
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

}
