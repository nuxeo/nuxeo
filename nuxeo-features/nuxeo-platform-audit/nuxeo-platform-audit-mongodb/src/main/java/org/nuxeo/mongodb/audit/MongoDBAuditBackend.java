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
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_DOC_PATH;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_DOC_UUID;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_EVENT_DATE;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_EVENT_ID;
import static org.nuxeo.mongodb.audit.LogEntryConstants.PROPERTY_REPOSITORY_ID;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.query.AuditQueryException;
import org.nuxeo.ecm.platform.audit.api.query.DateRangeParser;
import org.nuxeo.ecm.platform.audit.service.AbstractAuditBackend;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.BaseLogEntryProvider;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.mongodb.MongoDBComponent;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;
import org.nuxeo.runtime.mongodb.MongoDBSerializationHelper;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * Implementation of the {@link AuditBackend} interface using MongoDB persistence.
 *
 * @since 9.1
 */
public class MongoDBAuditBackend extends AbstractAuditBackend implements AuditBackend {

    private static final Log log = LogFactory.getLog(MongoDBAuditBackend.class);

    public static final String AUDIT_DATABASE_ID = "audit";

    public static final String COLLECTION_NAME_PROPERTY = "nuxeo.mongodb.audit.collection.name";

    public static final String DEFAULT_COLLECTION_NAME = "audit";

    public static final String SEQ_NAME = "audit";

    protected MongoCollection<Document> collection;

    protected MongoDBLogEntryProvider provider = new MongoDBLogEntryProvider();

    public MongoDBAuditBackend(NXAuditEventsService component, AuditBackendDescriptor config) {
        super(component, config);
    }

    @Override
    public int getApplicationStartedOrder() {
        DefaultComponent component = (DefaultComponent) Framework.getRuntime().getComponent(MongoDBComponent.NAME);
        return component.getApplicationStartedOrder() + 1;
    }

    @Override
    public void onApplicationStarted() {
        log.info("Activate MongoDB backend for Audit");
        // First retrieve the collection name
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        String collName = configurationService.getProperty(COLLECTION_NAME_PROPERTY, DEFAULT_COLLECTION_NAME);
        // Get a connection to MongoDB
        MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
        MongoDatabase database = mongoService.getDatabase(AUDIT_DATABASE_ID);
        collection = database.getCollection(collName);
        // TODO migration ?
    }

    @Override
    public void onShutdown() {
        if (collection != null) {
            collection = null;
        }
    }

    /**
     * @return the {@link MongoCollection} configured with audit settings.
     */
    public MongoCollection<Document> getAuditCollection() {
        return collection;
    }

    @Override
    public List<LogEntry> getLogEntriesFor(String uuid, String repositoryId) {
        Bson docFilter = Filters.eq(PROPERTY_DOC_UUID, uuid);
        Bson repoFilter = Filters.eq(PROPERTY_REPOSITORY_ID, repositoryId);
        Bson query = Filters.and(docFilter, repoFilter);
        return getLogEntries(query, false);
    }

    @Override
    public List<LogEntry> getLogEntriesFor(String uuid, Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        Bson docFilter = Filters.eq(PROPERTY_DOC_UUID, uuid);

        Bson filter;
        if (MapUtils.isEmpty(filterMap)) {
            filter = docFilter;
        } else {
            List<Bson> list = new ArrayList<>(Collections.singleton(docFilter));
            for (Entry<String, FilterMapEntry> entry : filterMap.entrySet()) {
                FilterMapEntry filterEntry = entry.getValue();
                list.add(Filters.eq(filterEntry.getColumnName(), filterEntry.getObject()));
            }
            filter = Filters.and(list);
        }
        return getLogEntries(filter, doDefaultSort);
    }

    @Override
    public LogEntry getLogEntryByID(long id) {
        Document document = collection.find(Filters.eq(MongoDBSerializationHelper.MONGODB_ID, Long.valueOf(id)))
                                      .first();
        if (document == null) {
            return null;
        }
        return MongoDBAuditEntryReader.read(document);
    }

    @Override
    public List<?> nativeQuery(String query, Map<String, Object> params, int pageNb, int pageSize) {
        Bson filter = buildFilter(query, params);
        logRequest(filter, pageNb, pageSize);
        FindIterable<Document> iterable = collection.find(filter).skip(pageNb * pageSize).limit(pageSize);
        return buildLogEntries(iterable);
    }

    public Bson buildFilter(String query, Map<String, Object> params) {
        if (params != null && params.size() > 0) {
            query = expandQueryVariables(query, params);
        }
        return Document.parse(query);
    }

    public String expandQueryVariables(String query, Object[] params) {
        Map<String, Object> qParams = new HashMap<>();
        for (int i = 0; i < params.length; i++) {
            query = query.replaceFirst("\\?", "\\${param" + i + "}");
            qParams.put("param" + i, params[i]);
        }
        return expandQueryVariables(query, qParams);
    }

    public String expandQueryVariables(String query, Map<String, Object> params) {
        if (params != null && params.size() > 0) {
            TextTemplate tmpl = new TextTemplate();
            // MongoDB date formatter - copied from org.bson.json.JsonWriter
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSS\'Z\'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            for (Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Calendar) {
                    tmpl.setVariable(key, dateFormat.format(((Calendar) value).getTime()));
                } else if (value instanceof Date) {
                    tmpl.setVariable(key, dateFormat.format(value));
                } else if (value != null) {
                    tmpl.setVariable(key, value.toString());
                }
            }
            query = tmpl.processText(query);
        }
        return query;
    }

    @Override
    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit, String[] categories, String path, int pageNb,
            int pageSize) {
        List<Bson> list = new ArrayList<>();
        if (eventIds != null && eventIds.length > 0) {
            if (eventIds.length == 1) {
                list.add(Filters.eq(PROPERTY_EVENT_ID, eventIds[0]));
            } else {
                list.add(Filters.in(PROPERTY_EVENT_ID, eventIds));
            }
        }
        if (categories != null && categories.length > 0) {
            if (categories.length == 1) {
                list.add(Filters.eq(PROPERTY_CATEGORY, categories[0]));
            } else {
                list.add(Filters.in(PROPERTY_CATEGORY, categories));
            }
        }
        if (path != null) {
            list.add(Filters.eq(PROPERTY_DOC_PATH, path));
        }
        if (limit != null) {
            list.add(Filters.lt(PROPERTY_EVENT_DATE, limit));
        }
        Bson filter = list.size() == 1 ? list.get(0) : Filters.and(list);
        logRequest(filter, pageNb, pageSize);
        FindIterable<Document> iterable = collection.find(filter).skip(pageNb * pageSize).limit(pageSize);
        return buildLogEntries(iterable);
    }

    @Override
    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange, String[] categories, String path,
            int pageNb, int pageSize) {
        // TODO maybe we can put this method in AbstratAuditBackend ?
        Date limit = null;
        if (dateRange != null) {
            try {
                limit = DateRangeParser.parseDateRangeQuery(new Date(), dateRange);
            } catch (AuditQueryException aqe) {
                aqe.addInfo("Wrong date range query. Query was " + dateRange);
                throw aqe;
            }
        }
        return queryLogsByPage(eventIds, limit, categories, path, pageNb, pageSize);
    }

    @Override
    public void addLogEntries(List<LogEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        UIDGeneratorService uidGeneratorService = Framework.getService(UIDGeneratorService.class);
        UIDSequencer seq = uidGeneratorService.getSequencer();

        List<Document> documents = new ArrayList<>(entries.size());
        for (LogEntry entry : entries) {
            entry.setId(seq.getNextLong(SEQ_NAME));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Indexing log enry Id: %s, with logDate : %s, for docUUID: %s ",
                        Long.valueOf(entry.getId()), entry.getLogDate(), entry.getDocUUID()));
            }
            documents.add(MongoDBAuditEntryWriter.asDocument(entry));
        }
        collection.insertMany(documents);
    }

    @Override
    public Long getEventsCount(String eventId) {
        return Long.valueOf(collection.count(Filters.eq("eventId", eventId)));
    }

    @Override
    public long syncLogCreationEntries(String repoId, String path, Boolean recurs) {
        return syncLogCreationEntries(provider, repoId, path, recurs);
    }

    @Override
    public ExtendedInfo newExtendedInfo(Serializable value) {
        return new MongoDBExtendedInfo(value);
    }

    private List<LogEntry> getLogEntries(Bson filter, boolean doDefaultSort) {
        Bson orderBy = null;
        if (doDefaultSort) {
            orderBy = Sorts.descending(PROPERTY_EVENT_DATE);
        }

        logRequest(filter, orderBy);
        FindIterable<Document> iterable = collection.find(filter).sort(orderBy);
        return buildLogEntries(iterable);
    }

    private List<LogEntry> buildLogEntries(FindIterable<Document> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false)
                            .map(MongoDBAuditEntryReader::read)
                            .collect(Collectors.toList());
    }

    private void logRequest(Bson filter, Bson orderBy) {
        if (log.isDebugEnabled()) {
            log.debug("MongoDB: FILTER " + filter + (orderBy == null ? "" : " ORDER BY " + orderBy));
        }
    }

    private void logRequest(Bson filter, int pageNb, int pageSize) {
        if (log.isDebugEnabled()) {
            log.debug("MongoDB: FILTER " + filter + " OFFSET " + pageNb + " LIMIT " + pageSize);
        }
    }

    public class MongoDBLogEntryProvider implements BaseLogEntryProvider {

        @Override
        public int removeEntries(String eventId, String pathPattern) {
            throw new UnsupportedOperationException("Not implemented yet!");
        }

        @Override
        public void addLogEntry(LogEntry logEntry) {
            List<LogEntry> entries = new ArrayList<>();
            entries.add(logEntry);
            addLogEntries(entries);
        }

        @Override
        public List<LogEntry> getLogEntriesFor(String uuid, String repositoryId) {
            throw new UnsupportedOperationException("Not implemented yet!");
        }

        @Override
        public List<LogEntry> getLogEntriesFor(String uuid) {
            throw new UnsupportedOperationException("Not implemented yet!");
        }

        @Override
        public List<LogEntry> getLogEntriesFor(String uuid, Map<String, FilterMapEntry> filterMap,
                boolean doDefaultSort) {
            throw new UnsupportedOperationException("Not implemented yet!");
        }

    }

}
