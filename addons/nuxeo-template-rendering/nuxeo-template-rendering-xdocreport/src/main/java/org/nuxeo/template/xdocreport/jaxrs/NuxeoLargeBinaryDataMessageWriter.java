/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.xdocreport.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingServletOutputStream;

import fr.opensagres.xdocreport.remoting.resources.domain.LargeBinaryData;
import fr.opensagres.xdocreport.remoting.resources.services.jaxrs.LargeBinaryDataMessageBodyWriter;

@Provider
public class NuxeoLargeBinaryDataMessageWriter extends LargeBinaryDataMessageBodyWriter {

    @Override
    public void writeTo(LargeBinaryData t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        BufferingServletOutputStream.stopBufferingThread();
        httpHeaders.add("X-Nuxeo", "WebEngine-JAXRS");
        super.writeTo(t, type, genericType, annotations, mediaType, httpHeaders, entityStream);
    }
}
