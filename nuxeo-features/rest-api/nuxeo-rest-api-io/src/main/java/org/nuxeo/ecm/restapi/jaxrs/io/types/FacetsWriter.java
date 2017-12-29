/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tdelprat
 */

package org.nuxeo.ecm.restapi.jaxrs.io.types;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.core.schema.types.CompositeType;

import com.fasterxml.jackson.core.JsonGenerator;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class FacetsWriter extends AbstractTypeDefWriter implements MessageBodyWriter<Facets> {

    @Override
    public void writeTo(Facets facets, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        JsonGenerator jg = getGenerator(entityStream);

        jg.writeStartArray();
        for (CompositeType facet : facets.facets) {
            jg.writeStartObject();
            writeFacet(jg, facet, true);
            jg.writeEndObject();
        }
        jg.writeEndArray();

        // flush
        jg.flush();
        jg.close();
        entityStream.flush();
    }

    @Override
    public long getSize(Facets arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type type, Annotation[] arg2, MediaType arg3) {
        return Facets.class == arg0;
    }

}
