/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.webengine.jaxrs.coreiodelegate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Reader;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.runtime.api.Framework;

/**
 * An abstract JAX-RS {@link MessageBodyWriter} that delegate marshalling to all nuxeo-core-io {@link Writer} and
 * {@link Reader} with conditions.
 *
 * @since 7.2
 */
public abstract class PartialCoreIODelegate implements MessageBodyWriter<Object>, MessageBodyReader<Object> {

    public static final String CONTENT_TYPE = "Content-Type";

    public static final String NUXEO_ENTITY = "; nuxeo-entity=";

    private Writer<?> writer = null;

    private Reader<?> reader = null;

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    @Context
    private HttpHeaders headers;

    /**
     * If it returns true, it delegates marshalling to {@link MarshallerRegistry}.
     *
     * @since 7.2
     */
    protected abstract boolean accept(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType);

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (accept(type, genericType, annotations, mediaType)) {
            RenderingContext ctx = RenderingContextWebUtils.getContext(request);
            MarshallerRegistry registry = Framework.getService(MarshallerRegistry.class);
            writer = registry.getWriter(ctx, type, genericType, mediaType);
            return writer != null;
        }
        return false;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (accept(type, genericType, annotations, mediaType)) {
            RenderingContext ctx = RenderingContextWebUtils.getContext(request);
            MarshallerRegistry registry = Framework.getService(MarshallerRegistry.class);
            reader = registry.getReader(ctx, type, genericType, mediaType);
            if (reader != null) {
                // backward compatibility for json document model marshalling
                DocumentModelJsonReaderLegacy.pushInstanceIfNeeded(ctx, request, headers.getRequestHeaders());
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
            WebApplicationException {
        if (writer != null) {
            ((Writer<Object>) writer).write(t, type, genericType, mediaType, entityStream);
        }
        RenderingContext ctx = RenderingContextWebUtils.getContext(request);
        response.setHeader(CONTENT_TYPE,
                mediaType + NUXEO_ENTITY + ctx.getParameter(RenderingContext.RESPONSE_HEADER_ENTITY_TYPE_KEY));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
            WebApplicationException {
        if (reader != null) {
            return reader.read(type, genericType, mediaType, entityStream);
        }
        return null;
    }

}
