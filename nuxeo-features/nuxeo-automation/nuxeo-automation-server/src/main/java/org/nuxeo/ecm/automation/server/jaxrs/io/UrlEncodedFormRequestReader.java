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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URLDecoder;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.server.jaxrs.ExecutionRequest;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;

/**
 * Reads {@link ExecutionRequest} from a urlencoded POST
 * (Needed for OAuth calls)
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
@Provider
@Consumes("application/x-www-form-urlencoded")
public class UrlEncodedFormRequestReader implements
        MessageBodyReader<ExecutionRequest> {

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

        String content = FileUtils.read(entityStream);
        String jsonString = null;
        if (content==null || content.isEmpty()) {
            // body was consumed by OAuth Filter and but Request parameters must have been cached
            // => need to get access to the request params
            jsonString = RequestContext.getActiveContext().getRequest().getParameter("jsondata");
        } else {
            if (content.startsWith("jsondata=")) {
                jsonString = content.substring(9);
                jsonString = URLDecoder.decode(jsonString, "UTF-8");
            } else {
                return null;
            }
        }

        if (jsonString==null) {
            return null;
        }

        return JsonRequestReader.readRequest(jsonString, httpHeaders);
    }

}
