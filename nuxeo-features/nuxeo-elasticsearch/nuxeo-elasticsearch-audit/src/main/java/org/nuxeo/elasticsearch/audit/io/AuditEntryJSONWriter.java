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
 *     Thierry Delprat
 */
package org.nuxeo.elasticsearch.audit.io;

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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.impl.blob.AbstractBlob;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class AuditEntryJSONWriter {

    protected static final Log log = LogFactory.getLog(AuditEntryJSONWriter.class);

    public static void asJSON(JsonGenerator jg, LogEntry logEntry) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("esAuditJson", Version.unknownVersion());
        module.addSerializer(Map.class, new MapEntrySerializer());
        module.addSerializer(AbstractBlob.class, new BinaryBlobEntrySerializer());
        objectMapper.registerModule(module);
        jg.setCodec(objectMapper);

        jg.writeStartObject();
        jg.writeStringField("entity-type", "logEntry");

        writeField(jg, LOG_CATEGORY, logEntry.getCategory());
        writeField(jg, LOG_PRINCIPAL_NAME, logEntry.getPrincipalName());
        writeField(jg, LOG_COMMENT, logEntry.getComment());
        writeField(jg, LOG_DOC_LIFE_CYCLE, logEntry.getDocLifeCycle());
        writeField(jg, LOG_DOC_PATH, logEntry.getDocPath());
        writeField(jg, LOG_DOC_TYPE, logEntry.getDocType());
        writeField(jg, LOG_DOC_UUID, logEntry.getDocUUID());
        writeField(jg, LOG_EVENT_ID, logEntry.getEventId());
        writeField(jg, LOG_REPOSITORY_ID, logEntry.getRepositoryId());
        jg.writeStringField(LOG_EVENT_DATE, ISODateTimeFormat.dateTime().print(new DateTime(logEntry.getEventDate())));
        jg.writeNumberField(LOG_ID, logEntry.getId());
        jg.writeStringField(LOG_LOG_DATE, ISODateTimeFormat.dateTime().print(new DateTime(logEntry.getLogDate())));
        Map<String, ExtendedInfo> extended = logEntry.getExtendedInfos();
        jg.writeObjectFieldStart(LOG_EXTENDED);
        for (String key : extended.keySet()) {
            ExtendedInfo ei = extended.get(key);
            if (ei != null && ei.getSerializableValue() != null) {
                Serializable value = ei.getSerializableValue();
                if (value instanceof String) {
                    String strValue = (String) value;
                    if (isJsonContent(strValue)) {
                        jg.writeFieldName(key);
                        jg.writeRawValue(strValue);
                    } else {
                        jg.writeStringField(key, strValue);
                    }
                } else {
                    try {
                        jg.writeObjectField(key, ei.getSerializableValue());
                    } catch (JsonMappingException e) {
                        log.error("No Serializer found.", e);
                    }
                }
            } else {
                jg.writeNullField(key);
            }
        }
        jg.writeEndObject();

        jg.writeEndObject();
        jg.flush();
    }

    /**
     * Helper method used to determine if a String field is actually nested JSON
     *
     * @since 7.4
     */
    protected static boolean isJsonContent(String value) {
        if (value != null) {
            value = value.trim();
            if (value.startsWith("{") && value.endsWith("}")) {
                return true;
            } else if (value.startsWith("[") && value.endsWith("]")) {
                return true;
            }
        }
        return false;
    }

    protected static void writeField(JsonGenerator jg, String name, String value) throws IOException {
        if (value == null) {
            jg.writeNullField(name);
        } else {
            jg.writeStringField(name, value);
        }
    }

    static class MapEntrySerializer extends JsonSerializer<Map> {

        @Override
        public void serialize(Map map, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                JsonProcessingException {
            jgen.writeStartObject();
            for (Object key : map.keySet()) {
                jgen.writeObjectField((String) key, map.get(key));
            }
            jgen.writeEndObject();
        }
    }

    static class BinaryBlobEntrySerializer extends JsonSerializer<AbstractBlob> {

        @Override
        public void serialize(AbstractBlob blob, JsonGenerator jgen, SerializerProvider provider)
                throws JsonGenerationException, IOException {
            // Do not serizalize
            jgen.writeNull();
        }
    }

}
