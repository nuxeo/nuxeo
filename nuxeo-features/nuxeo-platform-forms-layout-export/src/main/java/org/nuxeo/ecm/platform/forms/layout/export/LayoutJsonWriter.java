/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.platform.forms.layout.export;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;
import org.nuxeo.ecm.platform.forms.layout.api.Widget;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class LayoutJsonWriter extends AbstractJsonWriter<Layout> {

    @Override
    public void write(Layout entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("name", entity.getName());

        String type = entity.getType();
        if (type != null) {
            jg.writeStringField("type", type);
        }

        String typeCat = entity.getTypeCategory();
        if (typeCat != null) {
            jg.writeStringField("typeCategory", typeCat);
        }

        jg.writeStringField("mode", entity.getMode());

        String template = entity.getTemplate();
        if (template != null) {
            jg.writeStringField("template", template);
        }

        Map<String, Serializable> properties = entity.getProperties();
        if (MapUtils.isNotEmpty(properties)) {
            writeSerializableMapField("properties", properties, jg);
        }

        LayoutRow[] lRows = entity.getRows();
        if (lRows != null) {
            jg.writeArrayFieldStart("rows");
            for (LayoutRow lRow : lRows) {
                writeLayoutRow(lRow, jg);
            }
            jg.writeEndArray();
        }

        jg.writeEndObject();
    }

    protected void writeLayoutRow(LayoutRow layoutRow, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        String name = layoutRow.getName();
        if (name != null) {
            jg.writeStringField("name", name);
        }
        // fill selection info only if that's not the default value from the
        // definition
        if (layoutRow.isAlwaysSelected()) {
            jg.writeBooleanField("alwaysSelected", true);
        }
        if (!layoutRow.isSelectedByDefault()) {
            jg.writeBooleanField("selectedByDefault", false);
        }

        Map<String, Serializable> properties = layoutRow.getProperties();
        if (MapUtils.isNotEmpty(properties)) {
            writeSerializableMapField("properties", properties, jg);
        }
        Widget[] widgets = layoutRow.getWidgets();
        if (ArrayUtils.isNotEmpty(widgets)) {
            jg.writeArrayFieldStart("widgets");
            for (Widget widget : widgets) {
                writeWidget(widget, jg);
            }
            jg.writeEndArray();
        }
        jg.writeEndObject();
    }

    protected void writeWidget(Widget widget, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("name", widget.getName());
        jg.writeStringField("type", widget.getType());
        jg.writeStringField("typeCategory", widget.getTypeCategory());
        jg.writeStringField("mode", widget.getMode());
        jg.writeStringField("label", widget.getLabel());
        String helpLabel = widget.getHelpLabel();
        if (StringUtils.isNotBlank(helpLabel)) {
            jg.writeStringField("helpLabel", helpLabel);
        }
        jg.writeBooleanField("translated", widget.isTranslated());
        jg.writeBooleanField("handlingLabels", widget.isHandlingLabels());
        FieldDefinition[] fieldDefs = widget.getFieldDefinitions();
        if (ArrayUtils.isNotEmpty(fieldDefs)) {
            jg.writeArrayFieldStart("fields");
            for (FieldDefinition fieldDef : fieldDefs) {
                writeEntity(fieldDef, jg);
            }
            jg.writeEndArray();
        }

        Widget[] subWidgets = widget.getSubWidgets();
        if (ArrayUtils.isNotEmpty(subWidgets)) {
            jg.writeArrayFieldStart("subWidgets");
            for (Widget wDef : subWidgets) {
                writeWidget(wDef, jg);
            }
            jg.writeEndArray();
        }

        Map<String, Serializable> properties = widget.getProperties();
        if (MapUtils.isNotEmpty(properties)) {
            writeSerializableMapField("properties", properties, jg);
        }

        Map<String, Serializable> controls = widget.getControls();
        if (MapUtils.isNotEmpty(controls)) {
            writeSerializableMapField("controls", controls, jg);
        }

        WidgetSelectOption[] selectOptions = widget.getSelectOptions();
        if (ArrayUtils.isNotEmpty(selectOptions)) {
            writeSerializableListField("selectOptions", Arrays.asList(selectOptions), jg);
        }
        jg.writeEndObject();
    }

}
