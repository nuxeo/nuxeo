/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Nuxeo
 */
package org.nuxeo.ecm.core.event.async;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * A messageKey representing an Event.
 *
 * @since XXX
 */
public class EventRecord implements Serializable {

    private static final long serialVersionUID = 0L;

    public static final String SOURCE_EVENT = "sourceEvent";

    public static final String SOURCE_DOC_ID = "sourceId";

    public static final String SOURCE_DOC_REPO = "sourceRepository";

    public static final String SOURCE_DOC_TYPE = "sourceType";

    public static final String SOURCE_DOC_PATH = "sourcePath";

    public static final String SOURCE_DOC_TITLE = "sourceTitle";

    protected EventRecord() {
        // Empty constructor for Avro decoder
    }

    protected String id;

    protected String username;

    protected long time;

    protected Map<String, String> context = new HashMap<>();

    public String getEventName() {
        return getContext().get(SOURCE_EVENT);
    }

    public DocumentRef getDocumentSourceRef() {
        String sourceId = getDocumentSourceId();
        return sourceId.startsWith("/") ? new PathRef(sourceId) : new IdRef(sourceId);
    }

    public String getDocumentSourceId() {
        return context.get(SOURCE_DOC_ID);
    }

    public String getDocumentSourceType() {
        return context.get(SOURCE_DOC_TYPE);
    }

    public String getUsername() {
        return username;
    }

    public long getTime() {
        return time;
    }

    public String getRepository() {
        String repository = context.get(SOURCE_DOC_REPO);
        return StringUtils.isBlank(repository)
                ? Framework.getService(RepositoryManager.class).getDefaultRepositoryName()
                : repository;
    }

    public Map<String, String> getContext() {
        return new HashMap<>(context);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static EventRecordBuilder builder() {
        return new EventRecordBuilder();
    }

    public static class EventRecordBuilder {
        EventRecord record;

        protected EventRecordBuilder() {
            record = new EventRecord();
        }

        public EventRecordBuilder fromEvent(EventRecord record) {
            this.record = SerializationUtils.clone(record);
            return this;
        }

        public EventRecordBuilder withDocument(DocumentModel doc) {
            return withDocumentId(doc.getId()).withDocumentRepository(doc.getRepositoryName())
                                              .withDocumentType(doc.getType())
                                              .withContext(SOURCE_DOC_PATH, doc.getPathAsString())
                                              .withContext(SOURCE_DOC_TITLE, doc.getTitle());
        }

        public EventRecordBuilder withDocumentId(String docId) {
            withContext(SOURCE_DOC_ID, docId);
            return this;
        }

        public EventRecordBuilder withDocumentType(String docType) {
            withContext(SOURCE_DOC_TYPE, docType);
            return this;
        }

        public EventRecordBuilder withDocumentRepository(String repository) {
            withContext(SOURCE_DOC_REPO, repository);
            return this;
        }

        public EventRecordBuilder withEventName(String eventName) {
            withContext(SOURCE_EVENT, eventName);
            return this;
        }

        public EventRecordBuilder withUsername(String username) {
            record.username = username;
            return this;
        }

        public EventRecordBuilder withContext(String key, String value) {
            // Ensure value is not null, otherwise Avro will break the map encode
            if (value != null) {
                record.context.put(key, value);
            }
            return this;
        }

        public EventRecordBuilder withContext(Map<String, String> context) {
            record.context.putAll(context);
            return this;
        }

        public EventRecordBuilder withTime(long time) {
            record.time = time;
            return this;
        }

        public EventRecord build() {
            record.id = UUID.randomUUID().toString();
            return record;
        }
    }
}
