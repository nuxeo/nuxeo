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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;

/**
 * Base class for Json {@link Writer}.
 * <p>
 * This class provides a easy way to produce json and also provides the current context: {@link AbstractJsonWriter#ctx}.
 * It provides you a {@link JsonGenerator} to manage the marshalling.
 * </p>
 * <p>
 * The use of this class optimize the JsonFactory usage especially when aggregating marshallers.
 * </p>
 *
 * @param <EntityType> The Java type to marshall as Json.
 * @since 7.2
 */
@Supports(APPLICATION_JSON)
public abstract class AbstractJsonWriter<EntityType> implements Writer<EntityType> {

    /**
     * The current {@link RenderingContext}.
     */
    @Inject
    protected RenderingContext ctx;

    /**
     * The marshaller registry.
     */
    @Inject
    protected MarshallerRegistry registry;

    @Override
    public boolean accept(Class<?> clazz, Type genericType, MediaType mediatype) {
        return true;
    }

    @Override
    public void write(EntityType entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
            throws IOException {
        JsonGenerator jg = getGenerator(out, true);
        write(entity, jg);
        jg.flush();
    }

    /**
     * Implement this method to writes the entity in the provided {@link JsonGenerator}.
     * <p>
     * This method implementation can use injected properties.
     * </p>
     * <p>
     * The {@link JsonGenerator}'s flushing is done by this abstract class, it's also not not necessary to flush it. Do
     * not close the provided {@link JsonGenerator}. It may be used is another marshaller calling this one.
     * </p>
     *
     * @param entity The entity to marshall as Json.
     * @param jg The {@link JsonGenerator} used to produce Json output.
     * @since 7.2
     */
    public abstract void write(EntityType entity, JsonGenerator jg) throws IOException;

    /**
     * Delegates writing of an entity to the {@link MarshallerRegistry}. This will work if a Json {@link Writer} is
     * registered in the registry for the given clazz.
     *
     * @param fieldName The name of the Json field in which the entity will be wrote.
     * @param entity The entity to write.
     * @param jg The {@link JsonGenerator} used to write the given entity.
     * @since 7.2
     */
    protected void writeEntityField(String fieldName, Object entity, JsonGenerator jg) throws IOException {
        jg.writeFieldName(fieldName);
        writeEntity(entity, jg);
    }

    /**
     * Delegates writing of an entity to the {@link MarshallerRegistry}. This will work if a Json {@link Writer} is
     * registered in the registry for the given clazz.
     *
     * @param entity The entity to write.
     * @param jg The {@link JsonGenerator} used to write the given entity.
     * @since 7.2
     */
    protected void writeEntity(Object entity, JsonGenerator jg) throws IOException {
        writeEntity(entity, new OutputStreamWithJsonWriter(jg));
    }

    /**
     * Delegates writing of an entity to the {@link MarshallerRegistry}. This will work if a Json {@link Writer} is
     * registered in the registry for the given clazz.
     *
     * @param entity The entity to write.
     * @param out The {@link OutputStream} in which the given entity will be wrote.
     * @throws IOException If some i/o error append while writing entity.
     * @since 7.2
     */
    protected <ObjectType> void writeEntity(ObjectType entity, OutputStream out) throws IOException {
        @SuppressWarnings("unchecked")
        Class<ObjectType> clazz = (Class<ObjectType>) entity.getClass();
        Writer<ObjectType> writer = registry.getWriter(ctx, clazz, APPLICATION_JSON_TYPE);
        if (writer == null) {
            throw new MarshallingException("Unable to get a writer for Java type " + entity.getClass()
                    + " and mimetype " + APPLICATION_JSON_TYPE);
        }
        writer.write(entity, entity.getClass(), entity.getClass(), APPLICATION_JSON_TYPE, out);
    }

    /**
     * Get the current Json generator or create it if none was found.
     *
     * @param out The {@link OutputStream} on which the generator will generate Json.
     * @param getCurrentIfAvailable If true, try to get the current generator in the context.
     * @return The created generator.
     * @since 7.2
     */
    protected JsonGenerator getGenerator(OutputStream out, boolean getCurrentIfAvailable) throws IOException {
        if (getCurrentIfAvailable && out instanceof OutputStreamWithJsonWriter) {
            OutputStreamWithJsonWriter casted = (OutputStreamWithJsonWriter) out;
            return casted.getJsonGenerator();
        }
        return JsonFactoryProvider.get().createJsonGenerator(out);
    }

}
