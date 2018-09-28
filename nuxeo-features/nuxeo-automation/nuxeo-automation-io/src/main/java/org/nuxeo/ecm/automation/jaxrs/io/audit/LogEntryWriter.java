/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.jaxrs.io.audit;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.automation.jaxrs.io.EntityWriter;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 5.7.3 - LogEntry Writer for Audit.
 * @deprecated since 7.10 this marshaller was migrated to org.nuxeo.ecm.platform.audit.io.LogEntryJsonWriter. To use it
 *             in JAX-RS, register the {@link JsonCoreIODelegate} to forward the JAX-RS marshalling to nuxeo-core-io.
 */
@Deprecated
@Provider
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity" })
public class LogEntryWriter extends EntityWriter<LogEntry> {

    public static final String ENTITY_TYPE = "logEntry";

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

    @Override
    protected void writeEntityBody(JsonGenerator jg, LogEntry logEntry) throws IOException {
        jg.writeStringField("entity-type", "logEntry");
        jg.writeStringField("category", logEntry.getCategory());
        jg.writeStringField("principalName", logEntry.getPrincipalName());
        jg.writeStringField("comment", logEntry.getComment());
        jg.writeStringField("docLifeCycle", logEntry.getDocLifeCycle());
        jg.writeStringField("docPath", logEntry.getDocPath());
        jg.writeStringField("docType", logEntry.getDocType());
        jg.writeStringField("docUUID", logEntry.getDocUUID());
        jg.writeStringField("eventId", logEntry.getEventId());
        jg.writeStringField("repositoryId", logEntry.getRepositoryId());
        jg.writeStringField("eventDate", ISODateTimeFormat.dateTime().print(new DateTime(logEntry.getEventDate())));
        jg.writeNumberField("id", logEntry.getId());
        jg.writeStringField("logDate", ISODateTimeFormat.dateTime().print(new DateTime(logEntry.getLogDate())));
        writeExtendedInfos(jg, logEntry);
    }

    protected void writeExtendedInfos(JsonGenerator jg, LogEntry logEntry) throws IOException {
        Map<String, ExtendedInfo> extended = logEntry.getExtendedInfos();
        jg.writeObjectFieldStart("extended");
        for (String key : extended.keySet()) {
            ExtendedInfo ei = extended.get(key);
            if (ei != null && ei.getSerializableValue() != null) {
                writeExtendedInfo(jg, key, ei.getSerializableValue());
            } else {
                jg.writeNullField(key);
            }
        }
        jg.writeEndObject();
    }

    protected void writeExtendedInfo(JsonGenerator jg, String key, Serializable value) throws IOException {
        Class<?> clazz = value.getClass();
        if (Long.class.isAssignableFrom(clazz)) {
            jg.writeNumberField(key, (Long) value);
        } else if (Integer.class.isAssignableFrom(clazz)) {
            jg.writeNumberField(key, (Integer) value);
        } else if (Double.class.isAssignableFrom(clazz)) {
            jg.writeNumberField(key, (Double) value);
        } else if (Date.class.isAssignableFrom(clazz)) {
            jg.writeStringField(key, ISODateTimeFormat.dateTime().print(new DateTime(value)));
        } else if (String.class.isAssignableFrom(clazz)) {
            jg.writeStringField(key, (String) value);
        } else if (Boolean.class.isAssignableFrom(clazz)) {
            jg.writeBooleanField(key, (Boolean) value);
        } else {
            jg.writeStringField(key, value.toString());
        }
    }
}
