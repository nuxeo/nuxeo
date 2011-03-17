/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server.jaxrs.io;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.server.jaxrs.ExceptionHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Produces( { "application/json+nxentity", "application/json" })
public class JsonExceptionWriter implements MessageBodyWriter<ExceptionHandler> {

    public long getSize(ExceptionHandler arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return ExceptionHandler.class.isAssignableFrom(arg0);
    }

    public void writeTo(ExceptionHandler ee, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4,
            MultivaluedMap<String, Object> arg5, OutputStream arg6)
            throws IOException, WebApplicationException {
        JSONObject json = new JSONObject();
        json.element("entity-type", "exception");
        json.element("type", ee.getType());
        json.element("status", ee.getStatus());
        json.element("message", ee.getMessage());
        json.element("stack", ee.getSerializedStackTrace());
        arg6.write(json.toString(2).getBytes("UTF-8"));
    }

}
