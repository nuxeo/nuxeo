/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     slacoin
 */
package org.nuxeo.ecm.automation.jaxrs.io;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

@Produces(MediaType.APPLICATION_JSON)
public class JsonPrimitiveWriter implements MessageBodyWriter<Object> {

    @Override
    public long getSize(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> typeClass, Type arg1, Annotation[] arg2, MediaType arg3) {
        if (typeClass == String.class) {
            return true;
        }
        if (typeClass == Boolean.class) {
            return true;
        }
        if (Number.class.isAssignableFrom(typeClass)) {
            return true;
        }
        return false;
    }

    @Override
    public void writeTo(Object value, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
            MultivaluedMap<String, Object> arg5, OutputStream out) throws IOException, WebApplicationException {
        JsonWriter.writePrimitive(out, value);
    }

}
