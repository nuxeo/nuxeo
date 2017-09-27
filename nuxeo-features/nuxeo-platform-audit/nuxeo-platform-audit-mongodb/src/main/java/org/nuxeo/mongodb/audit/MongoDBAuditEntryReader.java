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

import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_CATEGORY;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_COMMENT;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_DOC_LIFE_CYCLE;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_DOC_PATH;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_DOC_TYPE;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_DOC_UUID;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_EVENT_DATE;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_EVENT_ID;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_EXTENDED;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_LOG_DATE;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_PRINCIPAL_NAME;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_REPOSITORY_ID;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.runtime.mongodb.MongoDBSerializationHelper;

import com.mongodb.DBObject;

/**
 * Reader for MongoDB Audit.
 *
 * @since 9.1
 */
public class MongoDBAuditEntryReader {

    private static final Log log = LogFactory.getLog(MongoDBAuditEntryReader.class);

    public static LogEntry read(Document doc) {
        LogEntryImpl entry = new LogEntryImpl();
        for (String key : doc.keySet()) {
            switch (key) {
            case MongoDBSerializationHelper.MONGODB_ID:
                entry.setId(doc.getLong(key).longValue());
                break;
            case PROPERTY_CATEGORY:
                entry.setCategory(doc.getString(key));
                break;
            case PROPERTY_PRINCIPAL_NAME:
                entry.setPrincipalName(doc.getString(key));
                break;
            case PROPERTY_COMMENT:
                entry.setComment(doc.getString(key));
                break;
            case PROPERTY_DOC_LIFE_CYCLE:
                entry.setDocLifeCycle(doc.getString(key));
                break;
            case PROPERTY_DOC_PATH:
                entry.setDocPath(doc.getString(key));
                break;
            case PROPERTY_DOC_TYPE:
                entry.setDocType(doc.getString(key));
                break;
            case PROPERTY_DOC_UUID:
                entry.setDocUUID(doc.getString(key));
                break;
            case PROPERTY_EVENT_ID:
                entry.setEventId(doc.getString(key));
                break;
            case PROPERTY_REPOSITORY_ID:
                entry.setRepositoryId(doc.getString(key));
                break;
            case PROPERTY_EVENT_DATE:
                entry.setEventDate(doc.getDate(key));
                break;
            case PROPERTY_LOG_DATE:
                entry.setLogDate(doc.getDate(key));
                break;
            case PROPERTY_EXTENDED:
                entry.setExtendedInfos(readExtendedInfo(doc.get(key, Document.class)));
                break;
            default:
                log.warn("Property with key '" + key + "' is not a known LogEntry property, skip it.");
                break;
            }
        }
        return entry;
    }

    public static Map<String, ExtendedInfo> readExtendedInfo(Document extInfos) {
        Map<String, ExtendedInfo> info = new HashMap<>();
        for (Entry<String, Object> entry : extInfos.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            ExtendedInfoImpl ei;
            if (value instanceof List || value instanceof DBObject) {
                ei = ExtendedInfoImpl.createExtendedInfo(value.toString());
            } else {
                ei = ExtendedInfoImpl.createExtendedInfo((Serializable) value);
            }
            info.put(key, ei);
        }
        return info;
    }

}
