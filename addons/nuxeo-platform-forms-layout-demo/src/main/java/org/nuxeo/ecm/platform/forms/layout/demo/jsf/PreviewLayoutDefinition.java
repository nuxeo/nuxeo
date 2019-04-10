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
package org.nuxeo.ecm.platform.forms.layout.demo.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.descriptors.FieldDescriptor;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class PreviewLayoutDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String widgetType;

    protected final List<String> fields;

    protected String label;

    protected String helpLabel;

    protected Boolean translated;

    protected Map<String, Serializable> properties;

    public PreviewLayoutDefinition(String widgetType, List<String> fields) {
        super();
        this.widgetType = widgetType;
        this.fields = fields;
    }

    public String getWidgetType() {
        return widgetType;
    }

    public List<String> getFields() {
        return fields;
    }

    public List<FieldDefinition> getFieldDefinitions() {
        if (fields != null) {
            List<FieldDefinition> res = new ArrayList<FieldDefinition>();
            for (String field : fields) {
                res.add(new FieldDescriptor(null, field));
            }
            return res;
        }
        return null;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHelpLabel() {
        return helpLabel;
    }

    public void setHelpLabel(String helpLabel) {
        this.helpLabel = helpLabel;
    }

    public Boolean getTranslated() {
        return translated;
    }

    public void setTranslated(Boolean translated) {
        this.translated = translated;
    }

    public Map<String, Serializable> getProperties() {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
            // rendered by default
            properties.put("rendered", Boolean.TRUE);
        }
        return properties;
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

}
