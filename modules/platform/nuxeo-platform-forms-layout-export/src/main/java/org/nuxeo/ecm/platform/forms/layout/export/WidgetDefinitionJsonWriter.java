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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.RenderingInfo;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetSelectOption;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class WidgetDefinitionJsonWriter extends AbstractLayoutJsonWriter<WidgetDefinition> {

    @Override
    public void write(WidgetDefinition entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("name", entity.getName());
        jg.writeStringField("type", entity.getType());
        String typeCategory = entity.getTypeCategory();
        if (StringUtils.isNotBlank(typeCategory)) {
            jg.writeStringField("typeCategory", typeCategory);
        }

        Map<String, String> labels = entity.getLabels();
        if (MapUtils.isNotEmpty(labels)) {
            writeSerializableMapField("labels", new TreeMap<>(labels), jg);
        }

        Map<String, String> helpLabels = entity.getHelpLabels();
        if (MapUtils.isNotEmpty(helpLabels)) {
            writeSerializableMapField("helpLabels", new TreeMap<>(helpLabels), jg);
        }

        jg.writeBooleanField("translated", entity.isTranslated());
        jg.writeBooleanField("handlingLabels", entity.isHandlingLabels());

        Map<String, String> widgetModes = entity.getModes();
        if (MapUtils.isNotEmpty(widgetModes)) {
            writeSerializableMapField("widgetModes", new TreeMap<>(widgetModes), jg);
        }

        FieldDefinition[] fieldDefinitions = entity.getFieldDefinitions();
        if (ArrayUtils.isNotEmpty(fieldDefinitions)) {
            jg.writeArrayFieldStart("fields");
            for (FieldDefinition fieldDefinition : fieldDefinitions) {
                writeEntity(fieldDefinition, jg);
            }
            jg.writeEndArray();
        }

        WidgetDefinition[] subWidgetDefinitions = entity.getSubWidgetDefinitions();
        if (ArrayUtils.isNotEmpty(subWidgetDefinitions)) {
            jg.writeArrayFieldStart("subWidgets");
            for (WidgetDefinition subWidgetDefinition : subWidgetDefinitions) {
                writeEntity(subWidgetDefinition, jg);
            }
            jg.writeEndArray();
        }

        WidgetReference[] subWidgetReferences = entity.getSubWidgetReferences();
        if (ArrayUtils.isNotEmpty(subWidgetReferences)) {
            jg.writeArrayFieldStart("subWidgetRefs");
            for (WidgetReference subWidgetReference : subWidgetReferences) {
                writeEntity(subWidgetReference, jg);
            }
            jg.writeEndArray();
        }

        Map<String, Map<String, Serializable>> properties = entity.getProperties();
        if (MapUtils.isNotEmpty(properties) && properties.values().stream().anyMatch(MapUtils::isNotEmpty)) {
            writeSerializableMapMapField("properties", cleanAndSort(properties), jg);
        }

        Map<String, Map<String, Serializable>> propertiesByWidgetMode = entity.getWidgetModeProperties();
        if (MapUtils.isNotEmpty(propertiesByWidgetMode)
                && propertiesByWidgetMode.values().stream().anyMatch(MapUtils::isNotEmpty)) {
            writeSerializableMapMapField("propertiesByWidgetMode", cleanAndSort(propertiesByWidgetMode), jg);
        }

        Map<String, Map<String, Serializable>> controls = entity.getControls();
        if (MapUtils.isNotEmpty(controls) && controls.values().stream().anyMatch(MapUtils::isNotEmpty)) {
            writeSerializableMapMapField("controls", cleanAndSort(controls), jg);
        }

        WidgetSelectOption[] selectOptions = entity.getSelectOptions();
        if (ArrayUtils.isNotEmpty(selectOptions)) {
            jg.writeArrayFieldStart("selectOptions");
            for (WidgetSelectOption selectOption : selectOptions) {
                writeEntity(selectOption, jg);
            }
            jg.writeEndArray();
        }

        Map<String, List<RenderingInfo>> renderingInfos = entity.getRenderingInfos();
        if (MapUtils.isNotEmpty(renderingInfos)
                && renderingInfos.values().stream().anyMatch(CollectionUtils::isNotEmpty)) {
            jg.writeObjectFieldStart("renderingInfos");
            // sort so that order is deterministic
            for (Entry<String, List<RenderingInfo>> entry : new TreeMap<>(renderingInfos).entrySet()) {
                writeSerializableListField(entry.getKey(), entry.getValue(), jg);
            }
            jg.writeEndObject();
        }

        List<String> aliases = entity.getAliases();
        if (CollectionUtils.isNotEmpty(aliases)) {
            writeSerializableListField("aliases", aliases, jg);
        }
        jg.writeEndObject();
    }

}
