/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs.io.directory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonDocumentWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.QName;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.runtime.api.Framework;

/**
 *
 *
 * @since 5.7.3
 */
public class DirectoryEntryWriter implements MessageBodyWriter<DirectoryEntry> {

    @Context
    JsonFactory jg;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return DirectoryEntry.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(DirectoryEntry t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1L;
    }

    @Override
    public void writeTo(DirectoryEntry directoryEntry, Class<?> type,
            Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
        JsonGenerator jg = JsonHelper.createJsonGenerator(entityStream);
        try {
            writeTo(jg, directoryEntry);
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * @param jg
     * @param directoryEntry
     * @throws IOException
     * @throws JsonGenerationException
     * @throws ClientException
     * @throws Exception
     */
    public static void writeTo(JsonGenerator jg, DirectoryEntry directoryEntry)
            throws JsonGenerationException, IOException, ClientException {

        boolean translateLabels = false;

        String directoryName = directoryEntry.getDirectoryName();

        jg.writeStartObject();
        jg.writeStringField("entity-type", "directory-entry");
        jg.writeStringField("directoryName", directoryName);
        jg.writeObjectFieldStart("properties");
        DirectoryService ds = Framework.getLocalService(DirectoryService.class);
        Directory directory = ds.getDirectory(directoryName);

        SchemaManager sm = Framework.getLocalService(SchemaManager.class);
        Schema schema = sm.getSchema(directory.getSchema());

        DocumentModel entry = directoryEntry.getDocumentModel();

        for (Field field : schema.getFields()) {
            QName fieldName = field.getName();
            String key = fieldName.getLocalName();
            Serializable value = entry.getPropertyValue(fieldName.getPrefixedName());
            if (translateLabels && "label".equals(key)) {
                value = translate((String) value);
            }

            jg.writeFieldName(key);
            JsonDocumentWriter.writePropertyValue(jg,
                    entry.getProperty(fieldName.getPrefixedName()), "");

        }

        jg.writeEndObject();

        jg.writeEndObject();
        jg.flush();
    }

    /**
     * @param value
     * @return
     *
     */
    private static Serializable translate(String value) {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

}
