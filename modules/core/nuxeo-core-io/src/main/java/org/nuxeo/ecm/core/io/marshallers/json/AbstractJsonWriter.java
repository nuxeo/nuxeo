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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.reflect.Supports;

import com.fasterxml.jackson.core.JsonGenerator;

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
        return JsonFactoryProvider.get().createGenerator(out);
    }

    /**
     * Writes a list of {@link Serializable}.
     *
     * @param fieldName The name of the Json field in which the serializables will be wrote.
     * @param serializables The serializables to write.
     * @param jg The {@link JsonGenerator} used to write the given serializables.
     * @since 10.1
     */
    protected <T extends Serializable> void writeSerializableListField(String fieldName, Collection<T> serializables,
            JsonGenerator jg) throws IOException {
        jg.writeArrayFieldStart(fieldName);
        for (T serializable : serializables) {
            writeSerializable(serializable, jg);
        }
        jg.writeEndArray();
    }

    /**
     * Writes a map whose values are {@link Serializable}.
     *
     * @param fieldName The name of the Json field in which the serializables will be wrote.
     * @param map The map to write.
     * @param jg The {@link JsonGenerator} used to write the given map.
     * @since 10.1
     */
    protected <T extends Serializable> void writeSerializableMapField(String fieldName, Map<String, T> map,
            JsonGenerator jg) throws IOException {
        jg.writeObjectFieldStart(fieldName);
        for (Entry<String, T> entry : map.entrySet()) {
            writeSerializableField(entry.getKey(), entry.getValue(), jg);
        }
        jg.writeEndObject();
    }

    /**
     * Writes a {@link Serializable}.
     * <p/>
     * This method will first try to cast value to {@link Collection}, array, {@link String}, {@link Boolean} and
     * {@link Number}. If none of previous cast could work, try to write it with marshallers
     *
     * @param fieldName The name of the Json field in which the serializable will be wrote.
     * @param value The value to write.
     * @param jg The {@link JsonGenerator} used to write the given serializable.
     * @since 10.1
     */
    protected void writeSerializableField(String fieldName, Serializable value, JsonGenerator jg) throws IOException {
        jg.writeFieldName(fieldName);
        writeSerializable(value, jg);
    }

    /**
     * Writes a {@link Serializable}.
     * <p/>
     * This method will first try to cast value to {@link Collection}, array, {@link String}, {@link Boolean} and
     * {@link Number}. If none of previous cast could work, try to write it with marshallers
     *
     * @param value The value to write.
     * @param jg The {@link JsonGenerator} used to write the given serializable.
     * @since 10.1
     */
    @SuppressWarnings("unchecked")
    protected void writeSerializable(Serializable value, JsonGenerator jg) throws IOException {
        if (value instanceof Collection) {
            jg.writeStartArray();
            for (Serializable serializable : (Collection<Serializable>) value) {
                writeSerializable(serializable, jg);
            }
            jg.writeEndArray();
        } else if (value instanceof Serializable[]) {
            jg.writeStartArray();
            for (Serializable serializable : (Serializable[]) value) {
                writeSerializable(serializable, jg);
            }
            jg.writeEndArray();
        } else if (value instanceof String) {
            jg.writeString((String) value);
        } else if (value instanceof Boolean) {
            jg.writeBoolean((boolean) value);
        } else if (value instanceof Number) {
            jg.writeNumber(value.toString());
        } else {
            // try with marshallers
            writeEntity(value, jg);
        }
    }

}
