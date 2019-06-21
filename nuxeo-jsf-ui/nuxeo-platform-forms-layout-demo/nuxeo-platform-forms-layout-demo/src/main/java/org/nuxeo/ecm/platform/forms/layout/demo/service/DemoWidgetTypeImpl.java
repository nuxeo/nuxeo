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
package org.nuxeo.ecm.platform.forms.layout.demo.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of the widget type demo
 *
 * @author Anahide Tchertchian
 */
public class DemoWidgetTypeImpl implements DemoWidgetType {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String label;

    protected String viewId;

    protected String category;

    protected String widgetTypeCategory;

    protected boolean previewEnabled;

    protected boolean previewHideViewMode;

    protected boolean previewHideEditMode;

    protected List<String> fields;

    protected Map<String, Serializable> defaultProperties;

    protected List<DemoLayout> demoLayouts;

    public DemoWidgetTypeImpl(String name, String label, String viewId, String category, String widgetTypeCategory,
            boolean previewEnabled, boolean previewHideViewMode, boolean previewHideEditMode, List<String> fields,
            Map<String, Serializable> defaultProperties, List<DemoLayout> demoLayouts) {
        super();
        this.name = name;
        this.label = label;
        this.viewId = viewId;
        this.category = category;
        this.widgetTypeCategory = widgetTypeCategory;
        this.previewEnabled = previewEnabled;
        this.previewHideViewMode = previewHideViewMode;
        this.previewHideEditMode = previewHideEditMode;
        this.fields = fields;
        this.defaultProperties = defaultProperties;
        this.demoLayouts = demoLayouts;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getViewId() {
        return viewId;
    }

    @Override
    public String getUrl() {
        return LayoutDemoManager.APPLICATION_PATH + viewId;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getWidgetTypeCategory() {
        return widgetTypeCategory;
    }

    @Override
    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    @Override
    public boolean isPreviewHideViewMode() {
        return previewHideViewMode;
    }

    @Override
    public boolean isPreviewHideEditMode() {
        return previewHideEditMode;
    }

    @Override
    public List<String> getFields() {
        return fields;
    }

    @Override
    public List<DemoLayout> getDemoLayouts() {
        return demoLayouts;
    }

    @Override
    public Map<String, Serializable> getDefaultProperties() {
        return defaultProperties;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DemoWidgetType) {
            DemoWidgetType oWidget = (DemoWidgetType) other;
            String oName = oWidget.getName();
            return Objects.equals(name, oName);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("DemoWidgetTypeImpl [name=%s, label=%s, " + "viewId=%s, category=%s]", name, label,
                viewId, category);
    }

}
