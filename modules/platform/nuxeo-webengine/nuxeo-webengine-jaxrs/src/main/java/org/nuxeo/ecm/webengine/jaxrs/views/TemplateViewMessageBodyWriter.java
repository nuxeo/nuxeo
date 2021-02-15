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
 *
 */

package org.nuxeo.ecm.webengine.jaxrs.views;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Provider
@Produces("*/*")
public class TemplateViewMessageBodyWriter implements MessageBodyWriter<TemplateView> {

    private static final Log log = LogFactory.getLog(TemplateViewMessageBodyWriter.class);

    // @ResourceContext private HttpServletRequest request;

    @Override
    public void writeTo(TemplateView t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        try {
            t.render(entityStream);
        } catch (RenderingException | IOException e) {
            log.error("Failed to render view: " + t.getUrl(), e);
            throw new IOException("Failed to render view: " + t.getUrl(), e);
        }
    }

    @Override
    public long getSize(TemplateView arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type type, Annotation[] arg2, MediaType arg3) {
        return TemplateView.class.isAssignableFrom(arg0);
    }

}
