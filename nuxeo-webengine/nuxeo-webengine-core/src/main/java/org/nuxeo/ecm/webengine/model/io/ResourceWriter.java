/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model.io;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Resource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Produces({"text/html", "*/*"})
public class ResourceWriter implements MessageBodyWriter<Resource> {

    public void writeTo(Resource t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {

        try {
            t.getView("index").render(entityStream);
        } catch (Throwable e) {
            throw WebException.wrap("Failed to render resource", e);
        }
    }

    public long getSize(Resource arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4) {
        // TODO Auto-generated method stub
        return -1;
    }

    public boolean isWriteable(Class<?> arg0, Type type, Annotation[] arg2,
            MediaType arg3) {
        return Resource.class.isAssignableFrom(arg0);
    }

}
