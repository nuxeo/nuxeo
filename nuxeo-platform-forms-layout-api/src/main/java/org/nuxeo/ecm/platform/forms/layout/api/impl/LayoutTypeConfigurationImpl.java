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
 * @since 5.9.6
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

    public String getSinceVersion() {
        return sinceVersion;
    }

    public void setSinceVersion(String sinceVersion) {
        this.sinceVersion = sinceVersion;
    }

    public String getDeprecatedVersion() {
        return deprecatedVersion;
    }

    public void setDeprecatedVersion(String deprecatedVersion) {
        this.deprecatedVersion = deprecatedVersion;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDemoId() {
        return demoId;
    }

    public void setDemoId(String demoId) {
        this.demoId = demoId;
    }

    public boolean isDemoPreviewEnabled() {
        return demoPreviewEnabled;
    }

    public void setDemoPreviewEnabled(boolean demoPreviewEnabled) {
        this.demoPreviewEnabled = demoPreviewEnabled;
    }

    public List<String> getSupportedModes() {
        return supportedModes;
    }

    public void setSupportedModes(List<String> supportedModes) {
        this.supportedModes = supportedModes;
    }

    public boolean isHandlingLabels() {
        return handlingLabels;
    }

    public void setHandlingLabels(boolean handlingLabels) {
        this.handlingLabels = handlingLabels;
    }

    public List<String> getSupportedControls() {
        return supportedControls;
    }

    public void setSupportedControls(List<String> supportedControls) {
        this.supportedControls = supportedControls;
    }

    public boolean isContainingForm() {
        return containingForm;
    }

    public void setContainingForm(boolean containingForm) {
        this.containingForm = containingForm;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public Map<String, List<LayoutDefinition>> getPropertyLayouts() {
        return propertyLayouts;
    }

    public List<LayoutDefinition> getPropertyLayouts(String mode,
            String additionalMode) {
        return getLayouts(propertyLayouts, mode, additionalMode);
    }

    public void setPropertyLayouts(
            Map<String, List<LayoutDefinition>> propertyLayouts) {
        this.propertyLayouts = propertyLayouts;
    }

    public Map<String, Map<String, Serializable>> getDefaultPropertyValues() {
        return defaultPropertyValues;
    }

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

    public void setDefaultPropertyValues(
            Map<String, Map<String, Serializable>> defaultPropertyValues) {
        this.defaultPropertyValues = defaultPropertyValues;
    }

    protected List<LayoutDefinition> getLayouts(
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

}
