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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.jaxrs.io.audit;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * @since 5.7.3 - LogEntry Writer for Audit.
 */
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class LogEntryWriter implements MessageBodyWriter<LogEntry> {

    @Context
    JsonFactory factory;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return LogEntry.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(LogEntry logEntry, Class<?> aClass, Type type,
            Annotation[] annotations, MediaType mediaType) {
        return -1L;
    }

    @Override
    public void writeTo(LogEntry logEntry, Class<?> aClass, Type type,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> stringObjectMultivaluedMap,
            OutputStream outputStream) throws IOException,
            WebApplicationException {
        try {
            writeLogEntry(
                    factory.createJsonGenerator(outputStream, JsonEncoding.UTF8),
                    logEntry);
        } catch (ClientException e) {
            throw new WebApplicationException(e);
        }
    }

    public static void writeLogEntry(JsonGenerator jg, LogEntry logEntry)
            throws ClientException, JsonGenerationException, IOException {
        jg.writeStartObject();
        jg.writeStringField("entity-type", "logentry");
        jg.writeStringField("category", logEntry.getCategory());
        jg.writeStringField("principalName", logEntry.getPrincipalName());
        jg.writeStringField("comment", logEntry.getComment());
        jg.writeStringField("docLifeCycle", logEntry.getDocLifeCycle());
        jg.writeStringField("docPath", logEntry.getDocPath());
        jg.writeStringField("docType", logEntry.getDocType());
        jg.writeStringField("docUUID", logEntry.getDocUUID());
        jg.writeStringField("eventId", logEntry.getEventId());
        jg.writeStringField("repositoryId", logEntry.getRepositoryId());
        jg.writeStringField(
                "eventDate",
                ISODateTimeFormat.dateTime().print(
                        new DateTime(logEntry.getEventDate())));
        jg.writeNumberField("id", logEntry.getId());
        jg.writeStringField(
                "logDate",
                ISODateTimeFormat.dateTime().print(
                        new DateTime(logEntry.getLogDate())));
        jg.writeEndObject();
        jg.flush();
    }

}
