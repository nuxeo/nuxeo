/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.platform.audit.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.codehaus.jackson.JsonGenerator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

/**
 * Convert {@link LogEntry} to Json.
 * <p>
 * This marshaller is enrichable: register class implementing {@link AbstractJsonEnricher} and managing {@link LogEntry}
 * .
 * </p>
 * <p>
 * This marshaller is also extensible: extend it and simply override
 * {@link ExtensibleEntityJsonWriter#extend(Object, JsonGenerator)}.
 * </p>
 * <p>
 * Format is:
 *
 * <pre>
 * {@code
 * {
 *   "entity-type":"logEntry",
 *   "category": "LOG_ENTRY_CATEGORY",
 *   "principalName": "LOG_ENTRY_PRINCIPAL",
 *   "comment": "LOG_ENTRY_COMMENT",
 *   "docLifeCycle": "DOC_LIFECYCLE",
 *   "docPath": "DOC_PATH",
 *   "docType": "DOC_TYPE",
 *   "docUUID": "DOC_UUID",
 *   "eventId": "EVENT_ID",
 *   "repositoryId": "REPO_ID",
 *   "eventDate": "LOG_EVENT_DATE",
 *   "logDate": "LOG_DATE"
 *             <-- contextParameters if there are enrichers activated
 *             <-- additional property provided by extend() method
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class LogEntryJsonWriter extends ExtensibleEntityJsonWriter<LogEntry> {

    public static final String ENTITY_TYPE = "logEntry";

    public LogEntryJsonWriter() {
        super(ENTITY_TYPE, LogEntry.class);
    }

    @Override
    protected void writeEntityBody(LogEntry logEntry, JsonGenerator jg) throws IOException {
        jg.writeNumberField("id", logEntry.getId());
        jg.writeStringField("category", logEntry.getCategory());
        jg.writeStringField("principalName", logEntry.getPrincipalName());
        jg.writeStringField("comment", logEntry.getComment());
        jg.writeStringField("docLifeCycle", logEntry.getDocLifeCycle());
        jg.writeStringField("docPath", logEntry.getDocPath());
        jg.writeStringField("docType", logEntry.getDocType());
        jg.writeStringField("docUUID", logEntry.getDocUUID());
        jg.writeStringField("eventId", logEntry.getEventId());
        jg.writeStringField("repositoryId", logEntry.getRepositoryId());
        DateTimeFormatter dateTime = ISODateTimeFormat.dateTime();
        jg.writeStringField("eventDate", dateTime.print(new DateTime(logEntry.getEventDate())));
        jg.writeStringField("logDate", dateTime.print(new DateTime(logEntry.getLogDate())));
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
