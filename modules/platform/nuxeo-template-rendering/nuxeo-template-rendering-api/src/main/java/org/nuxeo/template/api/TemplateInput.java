/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.template.api;

import static java.lang.Boolean.TRUE;
import static org.nuxeo.template.api.InputType.BooleanValue;
import static org.nuxeo.template.api.InputType.DateValue;
import static org.nuxeo.template.api.InputType.ListValue;
import static org.nuxeo.template.api.InputType.MapValue;
import static org.nuxeo.template.api.InputType.StringValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Represents input parameters of a Template. Inputs parameters have an {@link InputType}, a name an a value. Value can
 * be a xpath pointing to a {@link DocumentModel} property.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class TemplateInput implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String stringValue;

    protected Boolean booleanValue;

    protected Date dateValue;

    protected InputType type = StringValue;

    protected String source;

    protected String desciption;

    protected boolean readOnly;

    protected boolean autoLoop = false;

    private Map<String, TemplateInput> mapValue;

    public TemplateInput(String name) {
        this.name = name;
        this.stringValue = "";
    }

    public TemplateInput(String name, Object value) {
        this.name = name;
        if (value instanceof String) {
            stringValue = (String) value;
            type = StringValue;
        } else if (value instanceof Date) {
            dateValue = (Date) value;
            type = InputType.DateValue;
        } else if (value instanceof Boolean) {
            booleanValue = (Boolean) value;
            type = InputType.BooleanValue;
        }
    }

    public TemplateInput getCopy(boolean readOnly) {
        TemplateInput item = new TemplateInput(name);
        item.booleanValue = booleanValue;
        item.dateValue = dateValue;
        item.source = source;
        item.desciption = desciption;
        item.stringValue = stringValue;
        item.mapValue = mapValue;
        item.type = type;
        item.readOnly = readOnly;
        item.autoLoop = autoLoop;
        return item;
    }

    public TemplateInput update(TemplateInput other) {
        this.name = other.name;
        this.type = other.type;
        this.autoLoop = other.autoLoop;
        this.booleanValue = other.booleanValue;
        this.dateValue = other.dateValue;
        this.mapValue = other.mapValue;
        this.desciption = other.desciption;
        this.readOnly = other.readOnly;
        this.source = other.source;
        this.stringValue = other.stringValue;
        return this;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDesciption() {
        return desciption;
    }

    public void setDesciption(String desciption) {
        this.desciption = desciption;
    }

    @Override
    public String toString() {
        String str = name + " (" + type + ") : '";
        if (StringValue.equals(type) && stringValue != null) {
            str = str + stringValue;
        } else if (DateValue.equals(type) && dateValue != null) {
            str = str + dateValue.toString();
        } else if (BooleanValue.equals(type) && booleanValue != null) {
            str = str + booleanValue.toString();
        } else if ((MapValue.equals(type) || (ListValue.equals(type))) && mapValue != null) {
            str = str + mapValue.toString();
        } else {
            str = str + source;
        }
        return str + "'";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(Object stringValue) {
        this.stringValue = (String) stringValue;
    }

    public Boolean getBooleanValue() {
        if (booleanValue == null) {
            return Boolean.FALSE;
        }
        return booleanValue;
    }

    public void setBooleanValue(Object booleanValue) {
        if (booleanValue == null) {
            this.booleanValue = Boolean.FALSE;
        } else if (booleanValue instanceof String) {
            this.booleanValue = Boolean.valueOf((String) booleanValue);
        } else {
            this.booleanValue = (Boolean) booleanValue;
        }
    }

    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Date getDateValue() {
        if (dateValue == null) {
            return new Date();
        }
        return dateValue;
    }

    public void setDateValue(Object dateValue) {
        this.dateValue = (Date) dateValue;
    }

    public void setMapValue(Object value) {
        if (value instanceof List) {
            this.mapValue = new LinkedHashMap<>();
            for (TemplateInput child : (List<TemplateInput>) value) {
                mapValue.put(child.getName(), child);
            }
        } else if (value instanceof Map) {
            this.mapValue = (Map<String, TemplateInput>) value;
        }
    }

    public Map<String, TemplateInput> getMapValue() {
        return mapValue;
    }

    public List<TemplateInput> getListValue() {
        return new ArrayList<>(mapValue.values());
    }

    public InputType getType() {
        return type;
    }

    public String getTypeAsString() {
        if (type == null) {
            return "";
        }
        return type.toString();
    }

    public void setType(InputType type) {
        this.type = type;
    }

    public void setTypeAsString(String strType) {
        this.type = InputType.getByValue(strType);
    }

    public boolean isSimpleValue() {
        return !isSourceValue();
    }

    public boolean isSourceValue() {
        return (InputType.PictureProperty == type || InputType.DocumentProperty == type || InputType.Content == type);
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isSet() {
        return source != null || dateValue != null || booleanValue != null
                || (stringValue != null && !stringValue.isEmpty());
    }

    public boolean isAutoLoop() {
        return autoLoop;
    }

    public void setAutoLoop(boolean autoLoop) {
        this.autoLoop = autoLoop;
    }

    public static TemplateInput factory(String name, InputType type, Object value) {
        return factory(name, type, value, null, false, false);
    }

    public static TemplateInput factory(String name, InputType type, Object value, String description,
            Boolean isReadonly, Boolean isAutoloop) {
        TemplateInput param = new TemplateInput(name);
        param.setType(type);
        param.setReadOnly(TRUE.equals(isReadonly));
        param.setAutoLoop(TRUE.equals(isAutoloop));
        param.setDesciption(description);
        switch (type) {
        case StringValue:
            param.setStringValue(value);
            break;
        case BooleanValue:
            param.setBooleanValue(value);
            break;
        case DateValue:
            param.setDateValue(value);
            break;
        case MapValue:
        case ListValue:
            param.setMapValue(value);
            break;
        case DocumentProperty:
        case PictureProperty:
        case Content:
            param.setSource((String) value);
        }
        return param;
    }

}
