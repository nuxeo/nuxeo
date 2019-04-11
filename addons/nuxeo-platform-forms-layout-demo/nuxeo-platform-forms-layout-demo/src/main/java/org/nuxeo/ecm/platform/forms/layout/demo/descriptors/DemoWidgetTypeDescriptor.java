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
package org.nuxeo.ecm.platform.forms.layout.demo.descriptors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.forms.layout.demo.service.DemoLayout;
import org.nuxeo.ecm.platform.forms.layout.descriptors.PropertiesDescriptor;

/**
 * @author Anahide Tchertchian
 */
@XObject("widgetType")
public class DemoWidgetTypeDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    /**
     * Additional name that can be used when describing a widget type in a different category.
     *
     * @since 5.9.1
     */
    @XNode("@widgetTypeName")
    protected String widgetTypeName;

    @XNode("label")
    protected String label;

    @XNode("viewId")
    protected String viewId;

    @XNode("category")
    protected String category;

    @XNode("widgetTypeCategory")
    protected String widgetTypeCategory;

    @XNode("preview@enabled")
    protected boolean previewEnabled = false;

    @XNode("preview@hideViewMode")
    protected boolean previewHideViewMode = false;

    /**
     * @since 7.2
     */
    @XNode("preview@hideEditMode")
    protected boolean previewHideEditMode = false;

    @XNodeList(value = "preview/fields/field", type = ArrayList.class, componentType = String.class)
    protected List<String> fields;

    @XNode("preview/defaultProperties")
    protected PropertiesDescriptor defaultProperties;

    @XNodeList(value = "layouts/layout", type = ArrayList.class, componentType = DemoLayoutDescriptor.class)
    protected List<DemoLayoutDescriptor> demoLayouts;

    public String getName() {
        return name;
    }

    public String getWidgetTypeName() {
        if (widgetTypeName == null) {
            return getName();
        }
        return widgetTypeName;
    }

    public String getLabel() {
        return label;
    }

    public String getViewId() {
        return viewId;
    }

    public String getCategory() {
        return category;
    }

    /**
     * @since 5.7.3
     */
    public String getWidgetTypeCategory() {
        return widgetTypeCategory;
    }

    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public boolean isPreviewHideViewMode() {
        return previewHideViewMode;
    }

    /**
     * @since 7.2
     */
    public boolean isPreviewHideEditMode() {
        return previewHideEditMode;
    }

    public List<String> getFields() {
        return fields;
    }

    public Map<String, Serializable> getDefaultProperties() {
        if (defaultProperties == null) {
            return null;
        }
        return defaultProperties.getProperties();
    }

    public List<DemoLayout> getDemoLayouts() {
        List<DemoLayout> res = new ArrayList<>();
        res.addAll(demoLayouts);
        return res;
    }

}
