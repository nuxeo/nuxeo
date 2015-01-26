/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

    public static MarshallerRegistry registry;

    public static MarshallerRegistry getRegistry() {
        if (registry == null) {
            registry = Framework.getService(MarshallerRegistry.class);
        }
        return registry;
    }

    private Writer<?> writer = null;

    private Reader<?> reader = null;

    @Context
    private HttpServletRequest request;

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
            writer = getRegistry().getWriter(ctx, type, genericType, mediaType);
            return writer != null;
        }
        return false;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (accept(type, genericType, annotations, mediaType)) {
            RenderingContext ctx = RenderingContextWebUtils.getContext(request);
            reader = getRegistry().getReader(ctx, type, genericType, mediaType);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException,
            WebApplicationException {
        if (reader != null) {
            return ((Reader<Object>) reader).read(type, genericType, mediaType, entityStream);
        }
        return null;
    }

}
