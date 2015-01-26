/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import org.apache.commons.lang3.reflect.TypeUtils;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;

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
            jg.writeNumberField("currentPageSize", paginable.getCurrentPageSize());
            jg.writeNumberField("currentPageIndex", paginable.getCurrentPageIndex());
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
                    jg.writeObjectField("aggregations", paginable.getAggregates());
                }
            }
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
