/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.forms.layout.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for properties that should not be referenced using value expressions, see
 * {@link WebLayoutManager#referencePropertyAsExpression(String, Serializable, String, String, String, String)}
 *
 * @since 5.6
 */
@XObject("disabledPropertyRef")
public class DisabledPropertyRefDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@widgetType")
    protected String widgetType;

    /**
     * @since 5.7.3
     */
    @XNode("@widgetTypeCategory")
    protected String widgetTypeCategory;

    @XNode("@widgetMode")
    protected String widgetMode;

    @XNode("@template")
    protected String template;

    @XNode("@enabled")
    protected Boolean enabled = Boolean.TRUE;

    public String getId() {
        return name + "/" + widgetType + "/" + widgetTypeCategory + "/" + widgetMode + "/" + template;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWidgetType() {
        return widgetType;
    }

    public void setWidgetType(String widgetType) {
        this.widgetType = widgetType;
    }

    public String getWidgetTypeCategory() {
        return widgetTypeCategory;
    }

    public void setWidgetTypeCategory(String widgetTypeCategory) {
        this.widgetTypeCategory = widgetTypeCategory;
    }

    public String getWidgetMode() {
        return widgetMode;
    }

    public void setWidgetMode(String widgetMode) {
        this.widgetMode = widgetMode;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public boolean matches(String name, String widgetType, String widgetTypeCategory, String widgetMode,
            String template) {
        if (name != null && name.equals(this.name)) {
            if (matches(this.widgetType, widgetType)) {
                if (matches(this.widgetTypeCategory, widgetTypeCategory)) {
                    if (matches(this.widgetMode, widgetMode)) {
                        if (matches(this.template, template)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected boolean matches(String value1, String value2) {
        if ("*".equals(value1)) {
            return true;
        }
        if ((value1 == null && value2 == null) || (value1 == null) || (value1 != null && value1.equals(value2))
                || (value2 != null && value2.equals(value1))) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        buf.append(DisabledPropertyRefDescriptor.class.getName());
        buf.append(" {");
        buf.append("name=");
        buf.append(name);
        buf.append(", widgetType=");
        buf.append(widgetType);
        buf.append(", widgetTypeCategory=");
        buf.append(widgetTypeCategory);
        buf.append(", widgetMode=");
        buf.append(widgetMode);
        buf.append(", template=");
        buf.append(template);
        buf.append(", enabled=");
        buf.append(enabled);
        buf.append("}");

        return buf.toString();
    }

    @Override
    public DisabledPropertyRefDescriptor clone() {
        DisabledPropertyRefDescriptor clone = new DisabledPropertyRefDescriptor();
        clone.setEnabled(enabled);
        clone.setName(name);
        clone.setTemplate(template);
        clone.setWidgetMode(widgetMode);
        clone.setWidgetType(widgetType);
        clone.setWidgetTypeCategory(widgetTypeCategory);
        return clone;
    }

}
