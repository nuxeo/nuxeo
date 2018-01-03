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
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.forms.layout.api.FieldDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class WidgetTypeConfigurationJsonWriter extends AbstractLayoutJsonWriter<WidgetTypeConfiguration> {

    @Override
    public void write(WidgetTypeConfiguration entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("title", entity.getTitle());
        jg.writeStringField("description", entity.getDescription());
        String demoId = entity.getDemoId();
        if (demoId != null) {
            jg.writeObjectFieldStart("demo");
            jg.writeStringField("id", demoId);
            jg.writeBooleanField("previewEnabled", entity.isDemoPreviewEnabled());
            jg.writeEndObject();
        }
        jg.writeStringField("sinceVersion", entity.getSinceVersion());
        String deprVersion = entity.getDeprecatedVersion();
        if (StringUtils.isNotBlank(deprVersion)) {
            jg.writeStringField("deprecatedVersion", deprVersion);
        }
        Map<String, Serializable> confProperties = entity.getConfProperties();
        if (MapUtils.isNotEmpty(confProperties)) {
            writeSerializableMapField("confProperties", confProperties, jg);
        }

        List<String> supportedModes = entity.getSupportedModes();
        if (CollectionUtils.isNotEmpty(supportedModes)) {
            writeSerializableListField("supportedModes", supportedModes, jg);
        }

        if (entity.isAcceptingSubWidgets()) {
            jg.writeBooleanField("acceptingSubWidgets", entity.isAcceptingSubWidgets());
        }
        if (entity.isHandlingLabels()) {
            jg.writeBooleanField("handlingLabels", entity.isHandlingLabels());
        }
        List<String> supportedControls = entity.getSupportedControls();
        if (CollectionUtils.isNotEmpty(supportedControls)) {
            writeSerializableListField("supportedControls", supportedControls, jg);
        }
        if (entity.isContainingForm()) {
            jg.writeBooleanField("containingForm", true);
        }

        jg.writeObjectFieldStart("fields");
        jg.writeBooleanField("list", entity.isList());
        jg.writeBooleanField("complex", entity.isComplex());

        List<String> supportedTypes = entity.getSupportedFieldTypes();
        if (CollectionUtils.isNotEmpty(supportedTypes)) {
            writeSerializableListField("supportedTypes", supportedTypes, jg);
        }

        List<String> defaultTypes = entity.getDefaultFieldTypes();
        if (CollectionUtils.isNotEmpty(defaultTypes)) {
            writeSerializableListField("defaultTypes", defaultTypes, jg);
        }

        List<FieldDefinition> fieldDefinitions = entity.getDefaultFieldDefinitions();
        if (CollectionUtils.isNotEmpty(fieldDefinitions)) {
            writeSerializableListField("defaultConfiguration", fieldDefinitions, jg);
        }

        Map<String, List<LayoutDefinition>> fieldLayouts = entity.getFieldLayouts();
        if (MapUtils.isNotEmpty(fieldLayouts) && fieldLayouts.values().stream().anyMatch(CollectionUtils::isNotEmpty)) {
            // sort so that order is deterministic
            jg.writeObjectFieldStart("layouts");
            for (Entry<String, List<LayoutDefinition>> entry : new TreeMap<>(fieldLayouts).entrySet()) {
                writeSerializableListField(entry.getKey(), entry.getValue(), jg);
            }
            jg.writeEndObject();
        }
        // end of fields
        jg.writeEndObject();

        List<String> categories = entity.getCategories();
        if (CollectionUtils.isNotEmpty(categories)) {
            writeSerializableListField("categories", categories, jg);
        }

        Map<String, List<LayoutDefinition>> propertyLayouts = entity.getPropertyLayouts();
        if (MapUtils.isNotEmpty(propertyLayouts)
                && propertyLayouts.values().stream().anyMatch(CollectionUtils::isNotEmpty)) {
            // sort so that order is deterministic
            jg.writeObjectFieldStart("properties");
            jg.writeObjectFieldStart("layouts");
            for (Entry<String, List<LayoutDefinition>> entry : new TreeMap<>(propertyLayouts).entrySet()) {
                writeSerializableListField(entry.getKey(), entry.getValue(), jg);
            }
            jg.writeEndObject();
            jg.writeEndObject();
        }

        Map<String, Map<String, Serializable>> defaultPropertyValues = entity.getDefaultPropertyValues();
        if (MapUtils.isNotEmpty(defaultPropertyValues)
                && defaultPropertyValues.values().stream().anyMatch(MapUtils::isNotEmpty)) {
            writeSerializableMapMapField("defaultPropertyValues", cleanAndSort(defaultPropertyValues), jg);
        }

        Map<String, Map<String, Serializable>> defaultControlValues = entity.getDefaultControlValues();
        if (MapUtils.isNotEmpty(defaultControlValues)
                && defaultControlValues.values().stream().anyMatch(MapUtils::isNotEmpty)) {
            writeSerializableMapMapField("defaultControlValues", cleanAndSort(defaultControlValues), jg);
        }
        jg.writeEndObject();
    }

}
