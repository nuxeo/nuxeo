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

package org.nuxeo.ecm.webengine.rest.template;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ProduceMime;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.nuxeo.ecm.webengine.WebException;

import com.sun.jersey.impl.provider.entity.AbstractMessageReaderWriterProvider;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@ProduceMime("text/html")
public class TemplateProvider extends AbstractMessageReaderWriterProvider<Template> {

    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations) {
        return false;
    }

    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations) {
        return type == Template.class;
    }

    public void writeTo(Template t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
        try {
            t.render(entityStream);
        } catch (WebException e) {
            IOException ee = new IOException("Failed to render template");
            ee.initCause(e);
            throw ee;
        }
    }

    public Template readFrom(Class<Template> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        return null;
    }

}

