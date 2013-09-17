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
import org.nuxeo.ecm.webengine.WebException;

/**
 * Base class to write json entities
 *
 * @since 5.7.3
 */
public abstract class EntityWriter<T> implements MessageBodyWriter<T> {

    @Context
    protected JsonFactory factory;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        if(genericType instanceof ParameterizedType) {
            return false;
        } else {
            return ((Class)genericType).isAssignableFrom(type);
        }
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T entity, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {

        try {
            writeEntity(
                    factory.createJsonGenerator(entityStream, JsonEncoding.UTF8),
                    entity);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * @param jg a ready to user JsonGenerator
     * @param item
     * @throws IOException
     * @throws ClientException
     * @throws JsonGenerationException
     *
     */
    public void writeEntity(JsonGenerator jg, T item) throws IOException,
            ClientException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", getEntityType());

        writeEntityBody(jg, item);
        jg.writeEndObject();
        jg.flush();

    }

    /**
     * @param jg
     * @param item
     * @throws IOException
     * @throws ClientException
     * @throws JsonGenerationException
     *
     */
    abstract protected void writeEntityBody(JsonGenerator jg, T item)
            throws IOException, ClientException;

    /**
     * @return
     *
     */
    abstract protected String getEntityType();

}
