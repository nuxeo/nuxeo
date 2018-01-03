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
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOptions;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class WidgetSelectOptionJsonWriter extends AbstractJsonWriter<WidgetSelectOption> {

    @Override
    public void write(WidgetSelectOption entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        boolean isMulti = entity instanceof WidgetSelectOptions;
        jg.writeBooleanField("multiple", isMulti);
        Serializable value = entity.getValue();
        if (value != null) {
            writeSerializableField("value", value, jg);
        }
        String var = entity.getVar();
        if (var != null) {
            jg.writeStringField("var", var);
        }
        String itemLabel = entity.getItemLabel();
        if (itemLabel != null) {
            jg.writeStringField("itemLabel", itemLabel);
        }
        Map<String, String> labels = entity.getItemLabels();
        if (MapUtils.isNotEmpty(labels)) {
            writeSerializableMapField("itemLabels", labels, jg);
        }
        String itemValue = entity.getItemValue();
        if (itemValue != null) {
            jg.writeStringField("itemValue", itemValue);
        }
        Serializable itemDisabled = entity.getItemDisabled();
        if (itemDisabled != null) {
            writeSerializableField("itemDisabled", itemDisabled, jg);
        }
        Serializable itemRendered = entity.getItemRendered();
        if (itemRendered != null) {
            writeSerializableField("itemRendered", itemRendered, jg);
        }
        if (isMulti) {
            WidgetSelectOptions entities = (WidgetSelectOptions) entity;
            String ordering = entities.getOrdering();
            if (ordering != null) {
                jg.writeStringField("ordering", ordering);
            }
            Boolean caseSensitive = entities.getCaseSensitive();
            if (caseSensitive != null) {
                jg.writeBooleanField("caseSensitive", caseSensitive.booleanValue());
            }
        }
        jg.writeEndObject();
    }

}
