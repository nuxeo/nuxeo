/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutTypeConfiguration;

/**
 * @since 6.0
 */
public class LayoutTypeConfigurationImpl implements LayoutTypeConfiguration {

    private static final long serialVersionUID = 1L;

    protected String sinceVersion;

    protected String deprecatedVersion;

    protected String title;

    protected String description;

    protected String demoId;

    protected boolean demoPreviewEnabled = false;

    protected List<String> supportedModes;

    protected boolean handlingLabels = false;

    protected List<String> supportedControls;

    protected boolean containingForm = false;

    protected List<String> categories;

    protected Map<String, List<LayoutDefinition>> propertyLayouts;

    protected Map<String, Map<String, Serializable>> defaultPropertyValues;

    protected Map<String, List<LayoutDefinition>> fieldLayouts;

    public LayoutTypeConfigurationImpl() {
        super();
    }

    @Override
    public String getSinceVersion() {
        return sinceVersion;
    }

    public void setSinceVersion(String sinceVersion) {
        this.sinceVersion = sinceVersion;
    }

    @Override
    public String getDeprecatedVersion() {
        return deprecatedVersion;
    }

    public void setDeprecatedVersion(String deprecatedVersion) {
        this.deprecatedVersion = deprecatedVersion;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDemoId() {
        return demoId;
    }

    public void setDemoId(String demoId) {
        this.demoId = demoId;
    }

    @Override
    public boolean isDemoPreviewEnabled() {
        return demoPreviewEnabled;
    }

    public void setDemoPreviewEnabled(boolean demoPreviewEnabled) {
        this.demoPreviewEnabled = demoPreviewEnabled;
    }

    @Override
    public List<String> getSupportedModes() {
        return supportedModes;
    }

    public void setSupportedModes(List<String> supportedModes) {
        this.supportedModes = supportedModes;
    }

    @Override
    public boolean isHandlingLabels() {
        return handlingLabels;
    }

    public void setHandlingLabels(boolean handlingLabels) {
        this.handlingLabels = handlingLabels;
    }

    @Override
    public List<String> getSupportedControls() {
        return supportedControls;
    }

    public void setSupportedControls(List<String> supportedControls) {
        this.supportedControls = supportedControls;
    }

    @Override
    public boolean isContainingForm() {
        return containingForm;
    }

    public void setContainingForm(boolean containingForm) {
        this.containingForm = containingForm;
    }

    @Override
    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    @Override
    public Map<String, List<LayoutDefinition>> getPropertyLayouts() {
        return propertyLayouts;
    }

    @Override
    public List<LayoutDefinition> getPropertyLayouts(String mode, String additionalMode) {
        return getLayouts(propertyLayouts, mode, additionalMode);
    }

    public void setPropertyLayouts(Map<String, List<LayoutDefinition>> propertyLayouts) {
        this.propertyLayouts = propertyLayouts;
    }

    @Override
    public Map<String, Map<String, Serializable>> getDefaultPropertyValues() {
        return defaultPropertyValues;
    }

    @Override
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

    public void setDefaultPropertyValues(Map<String, Map<String, Serializable>> defaultPropertyValues) {
        this.defaultPropertyValues = defaultPropertyValues;
    }

    protected List<LayoutDefinition> getLayouts(Map<String, List<LayoutDefinition>> allLayouts, String mode,
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

    /**
     * @since 7.2
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LayoutTypeConfigurationImpl)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        LayoutTypeConfigurationImpl lc = (LayoutTypeConfigurationImpl) obj;
        return new EqualsBuilder().append(sinceVersion, lc.sinceVersion).append(deprecatedVersion, lc.deprecatedVersion).append(
                title, lc.title).append(description, lc.description).append(demoId, lc.demoId).append(
                demoPreviewEnabled, lc.demoPreviewEnabled).append(supportedModes, lc.supportedModes).append(
                handlingLabels, lc.handlingLabels).append(supportedControls, lc.supportedControls).append(
                containingForm, lc.containingForm).append(categories, lc.categories).append(propertyLayouts,
                lc.propertyLayouts).append(defaultPropertyValues, lc.defaultPropertyValues).append(fieldLayouts,
                lc.fieldLayouts).isEquals();
    }

}
