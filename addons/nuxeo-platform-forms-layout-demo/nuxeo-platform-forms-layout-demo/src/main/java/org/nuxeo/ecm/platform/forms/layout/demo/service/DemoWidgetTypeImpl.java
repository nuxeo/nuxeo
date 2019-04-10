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
package org.nuxeo.ecm.platform.forms.layout.demo.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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

    protected List<String> fields;

    protected Map<String, Serializable> defaultProperties;

    protected List<DemoLayout> demoLayouts;

    public DemoWidgetTypeImpl(String name, String label, String viewId,
            String category, String widgetTypeCategory, boolean previewEnabled,
            boolean previewHideViewMode, List<String> fields,
            Map<String, Serializable> defaultProperties,
            List<DemoLayout> demoLayouts) {
        super();
        this.name = name;
        this.label = label;
        this.viewId = viewId;
        this.category = category;
        this.widgetTypeCategory = widgetTypeCategory;
        this.previewEnabled = previewEnabled;
        this.previewHideViewMode = previewHideViewMode;
        this.fields = fields;
        this.defaultProperties = defaultProperties;
        this.demoLayouts = demoLayouts;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getViewId() {
        return viewId;
    }

    public String getUrl() {
        return LayoutDemoManager.APPLICATION_PATH + viewId;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String getWidgetTypeCategory() {
        return widgetTypeCategory;
    }

    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    @Override
    public boolean isPreviewHideViewMode() {
        return previewHideViewMode;
    }

    public List<String> getFields() {
        return fields;
    }

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
            if (name == null && oName != null) {
                return false;
            } else if (!name.equals(oName)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("DemoWidgetTypeImpl [name=%s, label=%s, "
                + "viewId=%s, category=%s]", name, label, viewId, category);
    }

}
