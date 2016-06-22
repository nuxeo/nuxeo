/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.forms.layout.service;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for properties that should not be referenced using value expressions, see
 * {@link WebLayoutManager#referencePropertyAsExpression(String, java.io.Serializable, String, String, String)}
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

    public boolean matches(String name, String widgetType, String widgetTypeCategory, String widgetMode, String template) {
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
