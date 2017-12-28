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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Reads {@link ExecutionRequest} from a urlencoded POST (Needed for OAuth calls)
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Provider
@Consumes("application/x-www-form-urlencoded")
public class UrlEncodedFormRequestReader implements MessageBodyReader<ExecutionRequest> {

    @Context
    protected HttpServletRequest request;

    @Context
    JsonFactory factory;

    public CoreSession getCoreSession() {
        return SessionFactory.getSession(request);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return (MediaType.APPLICATION_FORM_URLENCODED_TYPE.equals(mediaType) && ExecutionRequest.class.isAssignableFrom(type));
    }

    @Override
    public ExecutionRequest readFrom(Class<ExecutionRequest> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {

        String content = IOUtils.toString(entityStream, "UTF-8");
        String jsonString = null;
        if (content == null || content.isEmpty()) {
            // body was consumed by OAuth Filter and but Request parameters must
            // have been cached
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

        if (jsonString == null) {
            return null;
        }
        JsonParser jp = factory.createJsonParser(jsonString);
        try {
            return JsonRequestReader.readRequest(jp, httpHeaders, getCoreSession());
        } catch (IOException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

}
