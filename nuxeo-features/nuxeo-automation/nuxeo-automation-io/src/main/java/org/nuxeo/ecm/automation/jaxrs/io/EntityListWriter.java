/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.automation.core.util.Paginable;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Abstract class that knows how to serialize List of nuxeo entities. The implementing classes should only implement
 * {@link #getEntityType()} and {@link #writeItem(JsonGenerator, Object)}
 *
 * @since 5.7.3
 */
public abstract class EntityListWriter<T> extends EntityWriter<List<T>> {

    /**
     * Returns the entity-type value of the list (ie: users, groups....)
     */
    protected abstract String getEntityType();

    /**
     * Writes the item in a JsonGenerator.
     */
    protected abstract void writeItem(JsonGenerator jg, T item) throws IOException;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (!List.class.isAssignableFrom(type)) {
            return false;
        }

        // Verify the generic argument type
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type actualTypeArguments = paramType.getActualTypeArguments()[0];
            if (type == null) {
                throw new RuntimeException("Invalid class parameter type.");
            }
            return ((Class<?>) actualTypeArguments).isAssignableFrom(getItemClass());

        }
        return false;
    }

    private Class<?> getItemClass() {
        return (Class<?>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Override
    protected void writeEntityBody(JsonGenerator jg, List<T> list) throws IOException {
        writePaginableHeader(jg, list);
        writeHeader(jg, list);
        jg.writeArrayFieldStart("entries");
        for (T item : list) {
            writeItem(jg, item);
        }
        jg.writeEndArray();
    }

    protected void writePaginableHeader(JsonGenerator jg, List<T> list) throws IOException {
        if (list instanceof Paginable) {
            @SuppressWarnings("rawtypes")
            Paginable paginable = (Paginable) list;
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
            if (paginable.hasAggregateSupport() && paginable.getAggregates() != null
                    && !paginable.getAggregates().isEmpty()) {
                jg.writeObjectField("aggregations", paginable.getAggregates());
            }
        }
    }

    /**
     * Override this method to write into list header
     */
    protected void writeHeader(JsonGenerator jg, List<T> list) throws IOException {
    }

}
