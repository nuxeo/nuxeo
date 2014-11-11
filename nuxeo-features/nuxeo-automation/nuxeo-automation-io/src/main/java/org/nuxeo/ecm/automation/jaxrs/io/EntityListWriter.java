/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Abstract class that knows how to serialize List of nuxeo entities. The
 * implementing classes should only implement {@link #getEntityType()} and
 * {@link #writeItem(JsonGenerator, Object)}
 *
 * @since 5.7.3
 */
public abstract class EntityListWriter<T> extends EntityWriter<List<T>> {

    /**
     * Returns the entity-type value of the list (ie: users, groups....)
     *
     */
    protected abstract String getEntityType();

    /**
     * Writes the item in a JsonGenerator.
     */
    protected abstract void writeItem(JsonGenerator jg, T item)
            throws ClientException, IOException;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
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
    protected void writeEntityBody(JsonGenerator jg, List<T> list)
            throws IOException, ClientException {
        writePaginableHeader(jg, list);
        writeHeader(jg, list);
        jg.writeArrayFieldStart("entries");
        for (T item : list) {
            writeItem(jg, item);
        }
        jg.writeEndArray();
    }

    protected void writePaginableHeader(JsonGenerator jg, List<T> list)
            throws IOException {
        if (list instanceof Paginable) {
            Paginable paginable = (Paginable) list;
            jg.writeBooleanField("isPaginable", true);
            jg.writeNumberField("resultsCount", paginable.getResultsCount());
            jg.writeNumberField("pageSize", paginable.getPageSize());
            jg.writeNumberField("maxPageSize", paginable.getMaxPageSize());
            jg.writeNumberField("currentPageSize",
                    paginable.getCurrentPageSize());
            jg.writeNumberField("currentPageIndex",
                    paginable.getCurrentPageIndex());
            jg.writeNumberField("numberOfPages", paginable.getNumberOfPages());
            jg.writeBooleanField("isPreviousPageAvailable",
                    paginable.isPreviousPageAvailable());
            jg.writeBooleanField("isNextPageAvailable",
                    paginable.isNextPageAvailable());
            jg.writeBooleanField("isLastPageAvailable",
                    paginable.isLastPageAvailable());
            jg.writeBooleanField("isSortable", paginable.isSortable());
            jg.writeBooleanField("hasError", paginable.hasError());
            jg.writeStringField("errorMessage", paginable.getErrorMessage());
            if (paginable.hasAggregateSupport() && paginable.getAggregates() !=
                    null && !paginable.getAggregates().isEmpty()) {
                jg.writeObjectField("aggregations", paginable.getAggregates());
            }
        }
    }

    /**
     * Override this method to write into list header
     */
    protected void writeHeader(JsonGenerator jg, List<T> list)
            throws IOException {
    }

}
