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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Base class to write json entities
 *
 * @since 5.7.3
 */
public abstract class EntityWriter<T> implements MessageBodyWriter<T> {

    @Context
    protected JsonFactory factory;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (genericType instanceof ParameterizedType) {
            // See EntityListWriter to write Writer of parametrized class
            return false;
        } else {
            ParameterizedType ptype = (ParameterizedType) this.getClass().getGenericSuperclass();
            Type[] ts = ptype.getActualTypeArguments();
            Class c = (Class) ts[0];
            return c.isAssignableFrom(type);
        }
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T entity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {

        writeEntity(factory.createJsonGenerator(entityStream, JsonEncoding.UTF8), entity);
    }

    public void writeEntity(JsonGenerator jg, T item) throws IOException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", getEntityType());

        writeEntityBody(jg, item);
        jg.writeEndObject();
        jg.flush();

    }

    /**
     * Write the body of the entity. The object has already been opened and it entity-type rendered.
     */
    abstract protected void writeEntityBody(JsonGenerator jg, T item) throws IOException;

    /**
     * get the Entity type of the current entity type. It MUST follow camelCase notation
     *
     * @return the string representing the entity-type.
     */
    abstract protected String getEntityType();

}
