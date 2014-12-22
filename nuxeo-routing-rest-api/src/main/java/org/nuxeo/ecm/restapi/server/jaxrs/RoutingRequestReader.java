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
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
@Provider
public class RoutingRequestReader implements MessageBodyReader<RoutingRequest> {

    @Context
    private HttpServletRequest request;

    @Context
    JsonFactory factory;

    public static final MediaType targetMediaTypeNXReq = new MediaType("application", "json+nxroute");

    public static final MediaType targetMediaType = new MediaType("application", "json");

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return RoutingRequest.class.isAssignableFrom(type);
    }

    @Override
    public RoutingRequest readFrom(Class<RoutingRequest> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        JsonParser jp = factory.createJsonParser(entityStream);
        ObjectCodecService codecService = Framework.getLocalService(ObjectCodecService.class);
        try {
            return (RoutingRequest) codecService.read(jp, Thread.currentThread().getContextClassLoader(),  SessionFactory.getSession(request));
        } catch (ClassNotFoundException e) {
           throw new IllegalArgumentException(e);
        }
    }


}
