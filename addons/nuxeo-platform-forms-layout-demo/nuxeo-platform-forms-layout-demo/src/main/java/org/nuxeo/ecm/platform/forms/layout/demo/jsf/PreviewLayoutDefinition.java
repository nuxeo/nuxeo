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
package org.nuxeo.ecm.platform.forms.layout.demo.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.impl.FieldDefinitionImpl;

/**
 * Collects information to generate a layout definition from user information.
 *
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

    protected Boolean handlingLabels;

    protected Map<String, Serializable> defaultProperties;

    protected Map<String, Serializable> properties;

    protected List<Map<String, Serializable>> customProperties;

    protected List<WidgetDefinition> subWidgets;

    public PreviewLayoutDefinition(String widgetType, List<String> fields, Map<String, Serializable> defaultProperties) {
        super();
        this.widgetType = widgetType;
        this.fields = fields;
        this.defaultProperties = defaultProperties;
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
                res.add(new FieldDefinitionImpl(null, field));
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

    public Boolean getHandlingLabels() {
        return handlingLabels;
    }

    public void setHandlingLabels(Boolean handlingLabels) {
        this.handlingLabels = handlingLabels;
    }

    public List<WidgetDefinition> getSubWidgets() {
        return subWidgets;
    }

    public void setSubWidgets(List<WidgetDefinition> subWidgets) {
        this.subWidgets = subWidgets;
    }

    public Map<String, Serializable> getProperties() {
        if (properties == null) {
            properties = new HashMap<String, Serializable>();
            // fill with default properties
            if (defaultProperties != null) {
                properties.putAll(defaultProperties);
            }
        }
        return properties;
    }

    public void setProperties(Map<String, Serializable> properties) {
        this.properties = properties;
    }

    public List<Map<String, Serializable>> getCustomProperties() {
        if (customProperties == null) {
            customProperties = new ArrayList<Map<String, Serializable>>();
        }
        return customProperties;
    }

    public void setCustomProperties(List<Map<String, Serializable>> customProperties) {
        this.customProperties = customProperties;
    }

    public Map<String, Serializable> getWidgetProperties() {
        Map<String, Serializable> widgetProps = new HashMap<String, Serializable>();
        Map<String, Serializable> props = getProperties();
        if (props != null) {
            widgetProps.putAll(props);
        }
        List<Map<String, Serializable>> customProps = getCustomProperties();
        if (customProps != null) {
            widgetProps.putAll(convertCustomProperties(customProps, true));
        }
        return cleanUpProperties(widgetProps);
    }

    /**
     * Removes empty properties as the JSF component may not accept empty values for some properties like "converter" or
     * "validator".
     */
    protected Map<String, Serializable> cleanUpProperties(Map<String, Serializable> props) {
        Map<String, Serializable> res = new HashMap<String, Serializable>();
        if (props != null) {
            for (Map.Entry<String, Serializable> prop : props.entrySet()) {
                Serializable value = prop.getValue();
                if (value == null || (value instanceof String && StringUtils.isEmpty((String) value))) {
                    continue;
                }
                res.put(prop.getKey(), value);
            }
        }
        return res;
    }

    public Map<String, Serializable> getNewCustomProperty() {
        Map<String, Serializable> prop = new HashMap<String, Serializable>();
        prop.put("key", null);
        prop.put("value", null);
        return prop;
    }

    protected Map<String, Serializable> convertCustomProperties(List<Map<String, Serializable>> listProps,
            boolean ignoreErrors) throws ValidatorException {
        Map<String, Serializable> values = new HashMap<String, Serializable>();
        if (listProps != null) {
            for (Map<String, Serializable> entry : listProps) {
                String key = (String) entry.get("key");
                Serializable value = entry.get("value");
                if (key == null || key.trim().length() == 0) {
                    if (ignoreErrors) {
                        continue;
                    }
                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid empty key", null);
                    throw new ValidatorException(message);
                }
                if (values.containsKey(key)) {
                    if (ignoreErrors) {
                        continue;
                    }
                    FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, String.format(
                            "Duplicate key '%s'", key), null);
                    throw new ValidatorException(message);
                }
                values.put(key, value);
            }
        }
        return values;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void validateCustomProperties(FacesContext context, UIComponent component, Object value) {
        if (value != null && !(value instanceof List)) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid value: " + value, null);
            // also add global message
            context.addMessage(null, message);
            throw new ValidatorException(message);
        }
        List<Map<String, Serializable>> listValue = (List) value;
        // will throw an error
        convertCustomProperties(listValue, false);
    }

}
