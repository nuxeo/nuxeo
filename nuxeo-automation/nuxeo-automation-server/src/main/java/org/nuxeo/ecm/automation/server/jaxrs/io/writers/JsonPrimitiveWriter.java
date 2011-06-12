/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     slacoin
 */
package org.nuxeo.ecm.automation.server.jaxrs.io.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.nuxeo.ecm.automation.server.jaxrs.io.JsonWriter;

@Produces( { "application/json+nxentity", "application/json" })
public class JsonPrimitiveWriter implements MessageBodyWriter<Object> {

    @Override
    public long getSize(Object arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> typeClass, Type arg1,
            Annotation[] arg2, MediaType arg3) {
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
    public void writeTo(Object value, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4,
            MultivaluedMap<String, Object> arg5, OutputStream out)
            throws IOException, WebApplicationException {
        JsonWriter.writePrimitive(out, value);
    }

}
