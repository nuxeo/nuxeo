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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.impl.blob.AbstractBlob;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONWriter.BinaryBlobEntrySerializer;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONWriter.MapEntrySerializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class AuditEntryJSONReader {

    public static LogEntry read(String content) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("esAuditJson", Version.unknownVersion());
        module.addSerializer(Map.class, new MapEntrySerializer());
        module.addSerializer(AbstractBlob.class, new BinaryBlobEntrySerializer());
        objectMapper.registerModule(module);

        LogEntryImpl entry = new LogEntryImpl();

        JsonFactory factory = new JsonFactory();
        factory.setCodec(objectMapper);
        try (JsonParser jp = factory.createParser(content)) {

            JsonToken tok = jp.nextToken();

            // skip {
            if (jp.getCurrentToken() == JsonToken.START_OBJECT) {
                tok = jp.nextToken();
            }

            while (tok != null && tok != JsonToken.END_OBJECT) {
                String key = jp.getCurrentName();
                JsonToken token = jp.nextToken();
                if (token != JsonToken.VALUE_NULL) {
                    if (LOG_CATEGORY.equals(key)) {
                        entry.setCategory(token == JsonToken.VALUE_NULL ? null : jp.getText());
                    } else if (LOG_PRINCIPAL_NAME.equals(key)) {
                        entry.setPrincipalName(jp.getText());
                    } else if (LOG_COMMENT.equals(key)) {
                        entry.setComment(jp.getText());
                    } else if (LOG_DOC_LIFE_CYCLE.equals(key)) {
                        entry.setDocLifeCycle(jp.getText());
                    } else if (LOG_DOC_PATH.equals(key)) {
                        entry.setDocPath(jp.getText());
                    } else if (LOG_DOC_TYPE.equals(key)) {
                        entry.setDocType(jp.getText());
                    } else if (LOG_DOC_UUID.equals(key)) {
                        entry.setDocUUID(jp.getText());
                    } else if (LOG_EVENT_ID.equals(key)) {
                        entry.setEventId(jp.getText());
                    } else if (LOG_REPOSITORY_ID.equals(key)) {
                        entry.setRepositoryId(jp.getText());
                    } else if (LOG_ID.equals(key)) {
                        entry.setId(jp.getLongValue());
                    } else if (LOG_EVENT_DATE.equals(key)) {
                        entry.setEventDate(ISODateTimeFormat.dateTime().parseDateTime(jp.getText()).toDate());
                    } else if (LOG_LOG_DATE.equals(key)) {
                        entry.setLogDate(ISODateTimeFormat.dateTime().parseDateTime(jp.getText()).toDate());
                    } else if (LOG_EXTENDED.equals(key)) {
                        entry.setExtendedInfos(readExtendedInfo(jp, objectMapper));
                    }
                }
                tok = jp.nextToken();
            }
        }
        return entry;
    }

    public static Map<String, ExtendedInfo> readExtendedInfo(JsonParser jp, ObjectMapper objectMapper)
            throws IOException {

        Map<String, ExtendedInfo> info = new HashMap<>();

        JsonNode node = jp.readValueAsTree();

        Iterator<String> fieldsIt = node.fieldNames();

        while (fieldsIt.hasNext()) {
            String fieldName = fieldsIt.next();

            JsonNode field = node.get(fieldName);

            ExtendedInfoImpl ei = null;
            if (field.isObject() || field.isArray()) {
                ei = ExtendedInfoImpl.createExtendedInfo(objectMapper.writeValueAsString(field));
            } else {
                if (field.isInt() || field.isLong()) {
                    ei = ExtendedInfoImpl.createExtendedInfo(field.longValue());
                } else {
                    ei = ExtendedInfoImpl.createExtendedInfo(field.textValue());
                }
            }
            info.put(fieldName, ei);
        }
        return info;
    }

}
