/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io.documents;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

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
import org.nuxeo.ecm.automation.core.operations.business.adapter.BusinessAdapter;
import org.nuxeo.ecm.automation.io.services.codec.ObjectCodecService;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JAX-RS Message body reeader to decode BusinessAdapter
 *
 * @since 5.7.2
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class BusinessAdapterReader implements MessageBodyReader<BusinessAdapter> {

    @Context
    protected HttpServletRequest request;

    @Context
    JsonFactory factory;

    private CoreSession getCoreSession() {
        return SessionFactory.getSession(request);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return BusinessAdapter.class.isAssignableFrom(type);
    }

    @Override
    public BusinessAdapter readFrom(Class<BusinessAdapter> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
            MultivaluedMap<String, String> headers, InputStream in) throws IOException, WebApplicationException {
        return readRequest(in, headers);
    }

    public BusinessAdapter readRequest(InputStream in, MultivaluedMap<String, String> headers) throws IOException,
            WebApplicationException {
        // As stated in http://tools.ietf.org/html/rfc4627.html UTF-8 is the
        // default encoding for JSON content
        // TODO: add introspection on the first bytes to detect other admissible
        // json encodings, namely: UTF-8, UTF-16 (BE or LE), or UTF-32 (BE or LE)
        String content = IOUtils.toString(in, "UTF-8");
        if (content.isEmpty()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return readRequest(content, headers);
    }

    public BusinessAdapter readRequest(String content, MultivaluedMap<String, String> headers)
            throws WebApplicationException {
        try {
            return readRequest0(content, headers);
        } catch (WebApplicationException e) {
            throw e;
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
    }

    public BusinessAdapter readRequest0(String content, MultivaluedMap<String, String> headers) throws IOException {
        ObjectCodecService codecService = Framework.getService(ObjectCodecService.class);

        try (JsonParser jp = factory.createParser(content)) {
            JsonNode inputNode = jp.readValueAsTree();

            return (BusinessAdapter) codecService.readNode(inputNode, getCoreSession());
        }

    }

}
