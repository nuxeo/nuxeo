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
import static org.nuxeo.ecm.platform.forms.layout.export.LayoutExportConstants.CATEGORY_PARAMETER;
import static org.nuxeo.ecm.platform.forms.layout.export.LayoutExportConstants.LAYOUT_CONTEXT_PARAMETER;
import static org.nuxeo.ecm.platform.forms.layout.export.LayoutExportConstants.WIDGET_CONVERTERS_PARAMETER;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRowDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.RenderingInfo;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetReference;
import org.nuxeo.ecm.platform.forms.layout.api.converters.LayoutConversionContext;
import org.nuxeo.ecm.platform.forms.layout.api.converters.WidgetDefinitionConverter;
import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class LayoutDefinitionJsonWriter extends AbstractLayoutJsonWriter<LayoutDefinition> {

    private static final Log log = LogFactory.getLog(LayoutDefinitionJsonWriter.class);

    @Inject
    private LayoutStore webLayoutManager;

    @Override
    public void write(LayoutDefinition entity, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        String name = entity.getName();
        if (StringUtils.isNotBlank(name)) {
            jg.writeStringField("name", name);
        }

        String type = entity.getType();
        if (type != null) {
            jg.writeStringField("type", type);
        }

        String typeCat = entity.getTypeCategory();
        if (typeCat != null) {
            jg.writeStringField("typeCategory", typeCat);
        }

        Map<String, String> templates = entity.getTemplates();
        if (MapUtils.isNotEmpty(templates)) {
            writeSerializableMapField("templates", new TreeMap<>(templates), jg);
        }

        Map<String, Map<String, Serializable>> properties = entity.getProperties();
        if (MapUtils.isNotEmpty(properties) && properties.values().stream().anyMatch(MapUtils::isNotEmpty)) {
            writeSerializableMapMapField("properties", cleanAndSort(properties), jg);
        }

        // get category, layout context and widget converters from context
        String category = ctx.getParameter(CATEGORY_PARAMETER);
        List<WidgetDefinitionConverter> widgetConverters = ctx.getParameters(WIDGET_CONVERTERS_PARAMETER);
        LayoutConversionContext layoutCtx = ctx.getParameter(LAYOUT_CONTEXT_PARAMETER);

        LayoutRowDefinition[] rowDefinitions = entity.getRows();
        List<WidgetReference> widgetsToExport = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(rowDefinitions)) {
            jg.writeArrayFieldStart("rows");
            // use a counter to provide default name
            int rowIndex = -1;
            for (LayoutRowDefinition layoutRowDef : rowDefinitions) {
                rowIndex++;
                writeRawDefinition(layoutRowDef, layoutRowDef.getDefaultName(rowIndex), jg);

                WidgetReference[] widgets = layoutRowDef.getWidgetReferences();
                if (widgets != null) {
                    widgetsToExport.addAll(Arrays.asList(widgets));
                }
            }
            jg.writeEndArray();
        }

        if (!widgetsToExport.isEmpty()) {
            jg.writeArrayFieldStart("widgets");
            for (WidgetReference widgetRef : widgetsToExport) {
                WidgetDefinition widgetDefinition = getWidgetDefinition(widgetRef, category, entity, layoutCtx,
                        widgetConverters);
                if (widgetDefinition != null) {
                    writeEntity(widgetDefinition, jg);

                    // also export local subwidgets references
                    WidgetReference[] subWidgets = widgetDefinition.getSubWidgetReferences();
                    if (subWidgets != null) {
                        for (WidgetReference subWidgetRef : subWidgets) {
                            WidgetDefinition subWidgetDefinition = getWidgetDefinition(subWidgetRef, category, entity,
                                    layoutCtx, widgetConverters);
                            if (subWidgetDefinition != null) {
                                writeEntity(subWidgetDefinition, jg);
                            }
                        }
                    }
                }
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

    protected void writeRawDefinition(LayoutRowDefinition layoutRowDef, String defaultName, JsonGenerator jg)
            throws IOException {
        jg.writeStartObject();
        String name = layoutRowDef.getName();
        if (name != null) {
            jg.writeStringField("name", name);
        } else if (defaultName != null) {
            jg.writeStringField("name", defaultName);
        }
        // fill selection info only if that's not the default value from the definition
        if (layoutRowDef.isAlwaysSelected()) {
            jg.writeBooleanField("alwaysSelected", true);
        }
        if (!layoutRowDef.isSelectedByDefault()) {
            jg.writeBooleanField("selectedByDefault", false);
        }
        Map<String, Map<String, Serializable>> properties = layoutRowDef.getProperties();
        if (MapUtils.isNotEmpty(properties) && properties.values().stream().anyMatch(MapUtils::isNotEmpty)) {
            writeSerializableMapMapField("properties", cleanAndSort(properties), jg);
        }
        WidgetReference[] defWidgets = layoutRowDef.getWidgetReferences();
        if (ArrayUtils.isNotEmpty(defWidgets)) {
            writeSerializableListField("widgets", Arrays.asList(defWidgets), jg);
        }
        jg.writeEndObject();
    }

    protected WidgetDefinition getWidgetDefinition(WidgetReference widgetReference, String category,
            LayoutDefinition layoutDefinition, LayoutConversionContext ctx,
            List<WidgetDefinitionConverter> widgetConverters) {
        String widgetName = widgetReference.getName();
        WidgetDefinition widgetDefinition = layoutDefinition.getWidgetDefinition(widgetName);
        if (widgetDefinition == null) {
            String cat = widgetReference.getCategory();
            if (cat == null) {
                cat = category;
            }
            widgetDefinition = webLayoutManager.getWidgetDefinition(cat, widgetName);
        }
        if (widgetDefinition == null) {
            log.error(String.format("No definition found for widget '%s' in layout '%s' => cannot export", widgetName,
                    layoutDefinition.getName()));
        } else {
            if (widgetConverters != null) {
                for (WidgetDefinitionConverter conv : widgetConverters) {
                    widgetDefinition = conv.getWidgetDefinition(widgetDefinition, ctx);
                }
            }
        }
        return widgetDefinition;
    }

}
