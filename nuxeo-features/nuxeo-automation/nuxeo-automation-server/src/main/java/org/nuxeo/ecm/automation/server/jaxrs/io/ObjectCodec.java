/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs.io;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class ObjectCodec<T> {

    public static Class<?> findParametrizedType(Class<?> clazz) {
        Type superclass = clazz.getGenericSuperclass();
        while (superclass instanceof Class<?>) {
            superclass = ((Class<?>)superclass).getGenericSuperclass();
        }
        if (superclass == null) {
            throw new RuntimeException("Missing type parameter.");
        }
        Type type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
        if (!(type instanceof Class<?>)) {
            throw new RuntimeException("Invalid class parameter type. "+type);
        }
        return (Class<?>)type;
    }

    protected Class<T> type;

    @SuppressWarnings("unchecked")
    public ObjectCodec() {
        this.type = (Class<T>)findParametrizedType(getClass());
    }

    public ObjectCodec(Class<T> type) {
        this.type = type;
    }

    /**
     * Get this codec type. Implementors can override to return a short name.
     * The default name is the object type name.
     * @return
     */
    public String getType() {
        return type.getName();
    }

    /**
     * Whether this codec is a builtin codec
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
     * When the object codec is called the stream is positioned on the first value.
     * For inlined objects this is the first value after the "entity-type" property.
     * For non inlined objects this will be the object itself (i.e. '{' or '[')
     * @param jp
     * @return
     * @throws IOException
     */
    public T read(JsonParser jp) throws IOException {
        if (jp.getCodec() == null) {
            jp.setCodec(new ObjectMapper());
        }
        return jp.readValueAs(type);
    }

}
