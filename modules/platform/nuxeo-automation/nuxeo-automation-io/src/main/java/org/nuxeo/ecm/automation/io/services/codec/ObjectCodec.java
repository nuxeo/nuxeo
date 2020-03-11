/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.io.services.codec;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.nuxeo.ecm.core.api.CoreSession;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class ObjectCodec<T> {

    public static Class<?> findParametrizedType(Class<?> clazz) {
        Type superclass = clazz.getGenericSuperclass();
        while (superclass instanceof Class<?>) {
            superclass = ((Class<?>) superclass).getGenericSuperclass();
        }
        if (superclass == null) {
            throw new RuntimeException("Missing type parameter.");
        }
        Type type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
        if (!(type instanceof Class<?>)) {
            throw new RuntimeException("Invalid class parameter type. " + type);
        }
        return (Class<?>) type;
    }

    protected Class<T> type;

    @SuppressWarnings("unchecked")
    public ObjectCodec() {
        this.type = (Class<T>) findParametrizedType(getClass());
    }

    public ObjectCodec(Class<T> type) {
        this.type = type;
    }

    /**
     * Get this codec type. Implementors can override to return a short name. The default name is the object type name.
     *
     * @return
     */
    public String getType() {
        return type.getName();
    }

    /**
     * Whether this codec is a builtin codec
     *
     * @return
     */
    public boolean isBuiltin() {
        return false;
    }

    public Class<T> getJavaType() {
        return type;
    }

    public void write(JsonGenerator jg, T value) throws IOException {
        if (jg.getCodec() == null) {
            jg.setCodec(new ObjectMapper());
        }
        jg.writeObject(value);
    }

    /**
     * When the object codec is called the stream is positioned on the first value. For inlined objects this is the
     * first value after the "entity-type" property. For non inlined objects this will be the object itself (i.e. '{' or
     * '[')
     *
     * @param jp
     * @return
     * @throws IOException
     */
    public T read(JsonParser jp, CoreSession session) throws IOException {
        if (jp.getCodec() == null) {
            jp.setCodec(new ObjectMapper());
        }
        return jp.readValueAs(type);
    }

}
