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
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;

/**
 * Reads {@link ExecutionRequest} from a urlencoded POST (Needed for OAuth
 * calls)
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
@Provider
@Consumes("application/x-www-form-urlencoded")
public class UrlEncodedFormRequestReader implements
        MessageBodyReader<ExecutionRequest> {

    @Context
    protected HttpServletRequest request;

    @Context
    JsonFactory factory;

    public CoreSession getCoreSession() {
        return SessionFactory.getSession(request);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return (MediaType.APPLICATION_FORM_URLENCODED_TYPE.equals(mediaType) && ExecutionRequest.class.isAssignableFrom(type));
    }

    @Override
    public ExecutionRequest readFrom(Class<ExecutionRequest> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        String content = IOUtils.toString(entityStream, "UTF-8");
        String jsonString = null;
        if (content == null || content.isEmpty()) {
            // body was consumed by OAuth Filter and but Request parameters must
            // have been cached
            // => need to get access to the request params
            jsonString = RequestContext.getActiveContext().getRequest().getParameter(
                    "jsondata");
        } else {
            if (content.startsWith("jsondata=")) {
                jsonString = content.substring(9);
                jsonString = URLDecoder.decode(jsonString, "UTF-8");
            } else {
                return null;
            }
        }

        if (jsonString == null) {
            return null;
        }
        JsonParser jp = factory.createJsonParser(jsonString);
        try {
            return JsonRequestReader.readRequest(jp, httpHeaders,
                    getCoreSession());
        } catch (Exception e) {
            throw new WebApplicationException(
                    Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

}
