/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.api.QuickFilter;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Base class to convert {@link List} as json.
 * <p>
 * It follow the classic Nuxeo list format :
 *
 * <pre>
 * {
 *   "entity-type": "GIVEN_ENTITY_TYPE",
 *                                  <-- pagination info if available are here.
 *   "entries": [
 *     {...}, <-- A {@link Writer} must be able to manage this format.
 *     {...},
 *     ...
 *     {...}
 *   ]
 * }
 * </pre>
 * <p>
 * This list generates pagination information if the list is a {@link Paginable}.
 * </p>
 * <p>
 * This reader delegates the marshalling of entries to the {@link MarshallerRegistry}. A Json {@link Writer} compatible
 * with the required type must be registered.
 * </p>
 *
 * @param <EntityType> The type of the element of this list.
 * @since 7.2
 */
public abstract class DefaultListJsonWriter<EntityType> extends AbstractJsonWriter<List<EntityType>> {

    /**
     * The "entity-type" of the list.
     */
    private final String entityType;

    /**
     * The Java type of the element of this list.
     */
    private final Class<EntityType> elClazz;

    /**
     * The generic type of the element of this list.
     */
    private final Type elGenericType;

    /**
     * Use this constructor if the element of the list are not based on Java generic type.
     *
     * @param entityType The list "entity-type".
     * @param elClazz The class of the element of the list.
     */
    public DefaultListJsonWriter(String entityType, Class<EntityType> elClazz) {
        super();
        this.entityType = entityType;
        this.elClazz = elClazz;
        this.elGenericType = elClazz;
    }

    /**
     * Use this constructor if the element of the list are based on Java generic type.
     *
     * @param entityType The list "entity-type".
     * @param elClazz The class of the element of the list.
     * @param elGenericType The generic type of the list (you can use {@link TypeUtils#parameterize(Class, Type...) to
     *            generate it}
     */
    public DefaultListJsonWriter(String entityType, Class<EntityType> elClazz, Type elGenericType) {
        super();
        this.entityType = entityType;
        this.elClazz = elClazz;
        this.elGenericType = elGenericType;
    }

    @Override
    public void write(List<EntityType> list, JsonGenerator jg) throws IOException {
        jg.writeStartObject();
        ctx.setParameterValues(RenderingContext.RESPONSE_HEADER_ENTITY_TYPE_KEY, this.entityType);
        jg.writeStringField(ENTITY_FIELD_NAME, entityType);
        writePaginationInfos(list, jg);
        Writer<EntityType> documentWriter = registry.getWriter(ctx, elClazz, elGenericType, APPLICATION_JSON_TYPE);
        jg.writeArrayFieldStart("entries");
        for (EntityType entity : list) {
            documentWriter.write(entity, elClazz, elClazz, APPLICATION_JSON_TYPE, new OutputStreamWithJsonWriter(jg));
        }
        jg.writeEndArray();
        extend(list, jg);
        jg.writeEndObject();
    }

    private void writePaginationInfos(List<EntityType> list, JsonGenerator jg) throws IOException {
        if (list instanceof Paginable) {
            Paginable<?> paginable = (Paginable<?>) list;
            jg.writeBooleanField("isPaginable", true);
            jg.writeNumberField("resultsCount", paginable.getResultsCount());
            jg.writeNumberField("pageSize", paginable.getPageSize());
            jg.writeNumberField("maxPageSize", paginable.getMaxPageSize());
            jg.writeNumberField("resultsCountLimit", paginable.getResultsCountLimit());
            jg.writeNumberField("currentPageSize", paginable.getCurrentPageSize());
            jg.writeNumberField("currentPageIndex", paginable.getCurrentPageIndex());
            jg.writeNumberField("currentPageOffset", paginable.getCurrentPageOffset());
            jg.writeNumberField("numberOfPages", paginable.getNumberOfPages());
            jg.writeBooleanField("isPreviousPageAvailable", paginable.isPreviousPageAvailable());
            jg.writeBooleanField("isNextPageAvailable", paginable.isNextPageAvailable());
            jg.writeBooleanField("isLastPageAvailable", paginable.isLastPageAvailable());
            jg.writeBooleanField("isSortable", paginable.isSortable());
            jg.writeBooleanField("hasError", paginable.hasError());
            jg.writeStringField("errorMessage", paginable.getErrorMessage());
            // compat fields
            if (paginable instanceof DocumentModelList) {
                jg.writeNumberField("totalSize", ((DocumentModelList) paginable).totalSize());
            }
            jg.writeNumberField("pageIndex", paginable.getCurrentPageIndex());
            jg.writeNumberField("pageCount", paginable.getNumberOfPages());
            if (paginable.hasAggregateSupport()) {
                Map<String, Aggregate<? extends Bucket>> aggregates = paginable.getAggregates();
                if (aggregates != null && !paginable.getAggregates().isEmpty()) {
                    jg.writeObjectFieldStart("aggregations");
                    for (Entry<String, Aggregate<? extends Bucket>> e : aggregates.entrySet()) {
                        writeEntityField(e.getKey(), e.getValue(), jg);
                    }
                    jg.writeEndObject();
                }

            }
            List<QuickFilter> qfs = paginable.getActiveQuickFilters();
            List<QuickFilter> aqfs = paginable.getAvailableQuickFilters();
            if (aqfs != null && !aqfs.isEmpty()) {
                jg.writeArrayFieldStart("quickFilters");
                for (QuickFilter aqf : aqfs) {
                    jg.writeStartObject();
                    jg.writeStringField("name", aqf.getName());
                    jg.writeBooleanField("active", qfs.contains(aqf));
                    jg.writeEndObject();
                }
                jg.writeEndArray();
            }
        } else if (list instanceof PartialList) {
            PartialList<EntityType> partial = (PartialList<EntityType>) list;
            jg.writeNumberField("totalSize", partial.totalSize());
        }
    }

    /**
     * Override this method to write additional information in the list.
     *
     * @param list The list to marshal.
     * @param jg The {@link JsonGenerator} which point inside the list object at the end of standard properties.
     * @since 7.2
     */
    protected void extend(List<EntityType> list, JsonGenerator jg) throws IOException {
    }

}
