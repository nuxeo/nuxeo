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

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.types.Schema;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class DocumentTypesWriter extends AbstractTypeDefWriter implements
        MessageBodyWriter<DocumentTypes> {

    @Override
    public void writeTo(DocumentTypes typesDef, Class<?> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        try {

            JsonGenerator jg = getGenerator(entityStream);

            // start root
            jg.writeStartObject();

            // write types
            jg.writeObjectFieldStart("doctypes");
            for (DocumentType doctype : typesDef.getDocTypes()) {
                jg.writeObjectFieldStart(doctype.getName());
                writeDocType(jg, doctype, false);
                jg.writeEndObject();
            }
            jg.writeEndObject();

            // write schemas
            jg.writeObjectFieldStart("schemas");
            for (Schema schema : typesDef.getSchemas()) {
                writeSchema(jg, schema);
            }
            jg.writeEndObject();

            // end root
            jg.writeEndObject();

            // flush
            jg.flush();
            jg.close();
            entityStream.flush();
        } catch (Exception e) {
            throw new IOException("Failed to return types as JSON", e);
        }
    }

    @Override
    public long getSize(DocumentTypes arg0, Class<?> arg1, Type arg2,
            Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> arg0, Type type, Annotation[] arg2,
            MediaType arg3) {
        return DocumentTypes.class == arg0;
    }

}
