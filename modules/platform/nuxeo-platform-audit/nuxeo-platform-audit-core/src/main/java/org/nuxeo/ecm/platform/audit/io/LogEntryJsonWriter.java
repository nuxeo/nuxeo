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

import static org.nuxeo.common.utils.DateUtils.formatISODateTime;
import static org.nuxeo.common.utils.DateUtils.nowIfNull;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_CATEGORY;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_COMMENT;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_LIFE_CYCLE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_PATH;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_TYPE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EXTENDED;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_LOG_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_PRINCIPAL_NAME;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_REPOSITORY_ID;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.marshallers.json.enrichers.AbstractJsonEnricher;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

import com.fasterxml.jackson.core.JsonGenerator;

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
 * </p>
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
        jg.writeNumberField(LOG_ID, logEntry.getId());
        jg.writeStringField(LOG_CATEGORY, logEntry.getCategory());
        jg.writeStringField(LOG_PRINCIPAL_NAME, logEntry.getPrincipalName());
        jg.writeStringField(LOG_COMMENT, logEntry.getComment());
        jg.writeStringField(LOG_DOC_LIFE_CYCLE, logEntry.getDocLifeCycle());
        jg.writeStringField(LOG_DOC_PATH, logEntry.getDocPath());
        jg.writeStringField(LOG_DOC_TYPE, logEntry.getDocType());
        jg.writeStringField(LOG_DOC_UUID, logEntry.getDocUUID());
        jg.writeStringField(LOG_EVENT_ID, logEntry.getEventId());
        jg.writeStringField(LOG_REPOSITORY_ID, logEntry.getRepositoryId());
        jg.writeStringField(LOG_EVENT_DATE, formatISODateTime(nowIfNull(logEntry.getEventDate())));
        jg.writeStringField(LOG_LOG_DATE, formatISODateTime(nowIfNull(logEntry.getLogDate())));
        writeExtendedInfos(jg, logEntry);
    }

    protected void writeExtendedInfos(JsonGenerator jg, LogEntry logEntry) throws IOException {
        Map<String, ExtendedInfo> extended = logEntry.getExtendedInfos();
        jg.writeObjectFieldStart(LOG_EXTENDED);
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
            jg.writeStringField(key, formatISODateTime((Date) value));
        } else if (String.class.isAssignableFrom(clazz)) {
            jg.writeStringField(key, (String) value);
        } else if (Boolean.class.isAssignableFrom(clazz)) {
            jg.writeBooleanField(key, (Boolean) value);
        } else if (clazz.isArray() || List.class.isAssignableFrom(clazz)) {
            jg.writeObjectField(key, value);
        } else if (Map.class.isAssignableFrom(clazz)) {
            @SuppressWarnings("unchecked")
            Map<String, Serializable> map = (Map<String, Serializable>) value;
            jg.writeObjectFieldStart(key);
            for (Entry<String, Serializable> entry : map.entrySet()) {
                Serializable v = entry.getValue();
                if (v != null && !(v instanceof Blob)) {
                    writeExtendedInfo(jg, entry.getKey(), v);
                }
            }
            jg.writeEndObject();
        } else {
            // mainly blobs
            jg.writeStringField(key, value.toString());
        }
    }

}
