/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.webengine.model.Resource;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Produces({ "text/html", "*/*" })
public class ResourceWriter implements MessageBodyWriter<Resource> {

    @Override
    public void writeTo(Resource t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        t.getView("index").render(entityStream);
    }

    @Override
    public long getSize(Resource arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        // TODO Auto-generated method stub
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type type, Annotation[] arg2, MediaType arg3) {
        return Resource.class.isAssignableFrom(arg0);
    }

}
