/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *     vpasquier
 */
package org.nuxeo.ecm.webengine.app;

import org.nuxeo.ecm.webengine.WebException;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @since 6.0
 */
@Provider
@Produces({ MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_JSON + "+nxentity" })
public class JsonExceptionWriter implements MessageBodyWriter<WebException> {

    @Override
    public long getSize(WebException arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2,
            MediaType arg3) {
        return WebException.class.isAssignableFrom(arg0);
    }

    @Override
    public void writeTo(WebException webException, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType mediaType,
            MultivaluedMap<String, Object> arg5, OutputStream outputStream)
            throws IOException, WebApplicationException {
        JsonWebengineWriter.writeException(outputStream, webException, mediaType);
    }

}
