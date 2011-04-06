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

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.server.jaxrs.io.JsonMarshalling;
import org.nuxeo.runtime.api.Framework;

@Produces( { "application/json+nxentity", "application/json" })
public class JsonBuiltinWriter implements MessageBodyWriter<Object>{

    protected static JsonMarshalling marshalling() {
        return Framework.getLocalService(JsonMarshalling.class);
    }
    
    
    public JsonBuiltinWriter() {
    }
    
    @Override
    public boolean isWriteable(Class<?> clazz, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return marshalling().canMarshall(clazz);
    }

    @Override
    public long getSize(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3,
            MediaType arg4) {
        return -1;
    }
    

    @Override
    public void writeTo(Object value, Class<?> clazz, Type type, Annotation[] annotations,
            MediaType mediatype, MultivaluedMap<String, Object> httpHeaders,
            OutputStream out) throws IOException, WebApplicationException {
        JSONObject json = new JSONObject();
        json.element("entity-type", type(clazz));
        marshalling().write(clazz, json, value);
        out.write(json.toString(2).getBytes("UTF-8"));
    }
    
    public String type(Class<?> clazz) {
        return clazz.getSimpleName().toLowerCase();
    }
}
