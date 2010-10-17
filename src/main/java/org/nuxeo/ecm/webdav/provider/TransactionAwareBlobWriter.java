/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.webdav.provider;

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

import org.nuxeo.ecm.webengine.WebException;

@Provider
@Produces( { "*/*", "text/plain" })
public class TransactionAwareBlobWriter implements MessageBodyWriter<TransactionAwareBlob> {

    @Override
    public long getSize(TransactionAwareBlob blob, Class<?> type,
            Type genericType, Annotation[] annotations, MediaType mediaType) {
        long size = blob.getLength();
        return size <= 0L ? -1L : size;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return TransactionAwareBlob.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(TransactionAwareBlob blob, Class<?> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {

        try {
            blob.getBlob().transferTo(entityStream);
            entityStream.flush();
        } catch (Throwable e) {
            throw WebException.wrap("Failed to render resource", e);
        } finally {
            blob.commitOrRollback();
        }
    }

}
