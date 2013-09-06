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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Abstract class that knows how to serialize List of nuxeo entities. The
 * implementing classes should only implement {@link #getEntityType()} and
 * {@link #writeItem(JsonGenerator, Object)}
 *
 * @since 5.7.3
 */
public abstract class EntityListWriter<T> implements MessageBodyWriter<List<T>> {

    @Context
    JsonFactory factory;

    /**
     * Returns the entity-type value of the list (ie: users, groups....)
     * @return
     *
     */
    abstract protected String getEntityType();

    /**
     * Writes the item in a JsonGenerator.
     * @param jg
     * @param item
     * @throws IOException
     * @throws ClientException
     *
     */
    abstract protected void writeItem(JsonGenerator jg, T item)
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
            if (!(type instanceof Class<?>)) {
                throw new RuntimeException("Invalid class parameter type. "
                        + type);
            }
            return ((Class<?>) actualTypeArguments).isAssignableFrom(getItemClass());

        }
        return false;
    }

    private Class<?> getItemClass() {
        return (Class<?>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Override
    public long getSize(List<T> t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1L;
    }

    @Override
    public void writeTo(List<T> list, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
        try {
            writeList(factory.createJsonGenerator(entityStream, JsonEncoding.UTF8), list);
        } catch (ClientException e) {
            throw new WebApplicationException(e);
        }
    }

    /**
     * @param createGenerator
     * @param list
     * @throws IOException
     * @throws JsonGenerationException
     *
     */
    protected void writeList(JsonGenerator jg, List<T> list)
            throws ClientException, JsonGenerationException, IOException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", getEntityType());

        writeHeader(jg, list);
        jg.writeArrayFieldStart("items");
        for (T item : list) {
            writeItem(jg, item);
        }

        jg.writeEndArray();
        jg.writeEndObject();
        jg.flush();

    }

    /**
     * Override this method to write into list header
     * @param jg
     * @param list
     *
     */
    protected void writeHeader(JsonGenerator jg, List<T> list) {

    }

}
