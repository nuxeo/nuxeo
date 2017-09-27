/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.mongodb.audit;

import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_CATEGORY;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_COMMENT;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_LIFE_CYCLE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_PATH;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_TYPE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EXTENDED;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_LOG_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_PRINCIPAL_NAME;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_REPOSITORY_ID;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.runtime.mongodb.MongoDBSerializationHelper;

/**
 * Writer for MongoDB Audit.
 *
 * @since 9.1
 */
public class MongoDBAuditEntryWriter {

    public static Document asDocument(LogEntry logEntry) {
        Document document = new Document(MongoDBSerializationHelper.MONGODB_ID, Long.valueOf(logEntry.getId()));
        document.put(LOG_CATEGORY, logEntry.getCategory());
        document.put(LOG_PRINCIPAL_NAME, logEntry.getPrincipalName());
        document.put(LOG_COMMENT, logEntry.getComment());
        document.put(LOG_DOC_LIFE_CYCLE, logEntry.getDocLifeCycle());
        document.put(LOG_DOC_PATH, logEntry.getDocPath());
        document.put(LOG_DOC_TYPE, logEntry.getDocType());
        document.put(LOG_DOC_UUID, logEntry.getDocUUID());
        document.put(LOG_EVENT_ID, logEntry.getEventId());
        document.put(LOG_REPOSITORY_ID, logEntry.getRepositoryId());
        document.put(LOG_EVENT_DATE, logEntry.getEventDate());
        document.put(LOG_LOG_DATE, logEntry.getLogDate());

        Map<String, ExtendedInfo> extendedInfo = logEntry.getExtendedInfos();
        Document extended = new Document();
        for (Entry<String, ExtendedInfo> entry : extendedInfo.entrySet()) {
            String key = entry.getKey();
            ExtendedInfo ei = entry.getValue();
            if (ei != null && ei.getSerializableValue() != null) {
                Object value = ei.getSerializableValue();
                if (value instanceof Object[]) {
                    value = Arrays.asList((Object[]) value);
                }
                extended.put(key, value);
            } else {
                extended.put(key, null);
            }
        }
        document.put(LOG_EXTENDED, extended);
        return document;
    }

}
