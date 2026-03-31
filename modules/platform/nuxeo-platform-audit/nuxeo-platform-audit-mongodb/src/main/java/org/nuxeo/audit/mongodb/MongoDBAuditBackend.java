/*
 * (C) Copyright 2017-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.audit.mongodb;

import static com.mongodb.client.model.Projections.include;
import static org.nuxeo.audit.api.LogEntryConstants.LOG_ID;
import static org.nuxeo.ecm.core.uidgen.KeyValueStoreUIDSequencer.DEFAULT_STORE_NAME;
import static org.nuxeo.runtime.mongodb.MongoDBSerializationHelper.MONGODB_ID;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.LogEntryList;
import org.nuxeo.audit.service.AbstractAuditBackend;
import org.nuxeo.audit.service.AuditBackend;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.ecm.core.api.CursorResult;
import org.nuxeo.ecm.core.api.CursorService;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.storage.mongodb.query.MongoDBQuerySearchBuilder;
import org.nuxeo.ecm.core.storage.mongodb.query.MongoDBSearchConverter;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.mongodb.MongoDBSerializationHelper;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * Implementation of the {@link AuditBackend} interface using MongoDB persistence.
 *
 * @since 9.1
 */
public class MongoDBAuditBackend extends AbstractAuditBackend
        implements AuditBackend.CursorServiceScroll<MongoCursor<Document>, Document> {

    private static final Logger log = LogManager.getLogger(MongoDBAuditBackend.class);

    public static final String SEQ_NAME = "audit";

    protected final MongoCollection<Document> collection;

    protected final CursorService<MongoCursor<Document>, Document, String> cursorService;

    protected final CursorService<MongoCursor<Document>, Document, String> storageCursorService;

    /**
     * @since 2025.0
     */
    public MongoDBAuditBackend(MongoCollection<Document> collection) {
        this.collection = collection;
        initUIDSequencer(collection);
        cursorService = new CursorService<>(
                document -> String.valueOf(document.getLong(MongoDBSerializationHelper.MONGODB_ID)));
        this.storageCursorService = new CursorService<>(doc -> {
            Object id = doc.remove(MONGODB_ID);
            if (id != null) {
                doc.put(LOG_ID, id);
            }
            return doc.toJson();
        });
    }

    /**
     * @return the {@link MongoCollection} configured with audit settings.
     */
    public MongoCollection<Document> getAuditCollection() {
        return collection;
    }

    /**
     * Ensures the audit sequence returns an UID greater or equal than the maximum log entry id.
     */
    protected static void initUIDSequencer(MongoCollection<Document> collection) {
        var lastLogEntry = collection.find(Filters.empty()).sort(Sorts.descending(MONGODB_ID)).limit(1).first();
        if (lastLogEntry == null) {
            log.debug("There's no LogEntries, skip UIDSequencer initialization");
            return;
        }
        Long lastLogEntryId = lastLogEntry.getLong(MONGODB_ID);

        var uidSequencer = Framework.getService(UIDSequencer.class);
        long currentSequenceValue = uidSequencer.getCurrent(SEQ_NAME);
        if (currentSequenceValue < lastLogEntryId) {
            log.info("UID returned by sequence: {} is: {}, initializing sequence to: {}", SEQ_NAME,
                    currentSequenceValue, lastLogEntryId);
            uidSequencer.initSequence(SEQ_NAME, lastLogEntryId);
        }
    }

    @Override
    public LogEntryList queryLogs(QueryBuilder query) {
        // prepare parameters
        long offset = query.offset();
        long limit = query.limit();

        // create MongoDB filter & order
        var builder = new MongoDBQuerySearchBuilder(new MongoDBSearchConverter(LOG_ID), query);
        builder.walk();
        var filter = builder.getFilter();
        var sort = builder.getSort();

        logRequest(filter, sort);
        FindIterable<Document> iterable = collection.find(filter).sort(sort).skip((int) offset).limit((int) limit);
        var result = buildLogEntries(iterable);
        long totalSize = PageProvider.UNKNOWN_SIZE;
        if (query.countTotal()) {
            totalSize = collection.countDocuments(filter);
        }
        return new LogEntryList(result, totalSize);
    }

    /** @since 2025.18 */
    @Override
    public CursorResult<MongoCursor<Document>, Document> scrollLogIdsAsCursor(QueryBuilder query, int batchSize,
            Duration keepAlive) {
        // create MongoDB filter & order
        var builder = new MongoDBQuerySearchBuilder(new MongoDBSearchConverter(LOG_ID), query);
        builder.walk();
        var filter = builder.getFilter();
        var sort = builder.getSort();

        logRequest(filter, sort);
        MongoCursor<Document> cursor = collection.find(filter)
                                                 .sort(sort)
                                                 .projection(include(LOG_ID))
                                                 .batchSize(batchSize)
                                                 .iterator();
        return new CursorResult<>(cursor, batchSize, (int) keepAlive.toSeconds());
    }

    @Override
    public LogEntry getLogEntryByID(long id) {
        Document document = collection.find(Filters.eq(MONGODB_ID, id)).first();
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
        if (params != null && !params.isEmpty()) {
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
        if (params != null && !params.isEmpty()) {
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
    public void insertLogs(Collection<LogEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        List<Document> documents = new ArrayList<>(entries.size());
        for (var entry : entries) {
            if (entry.getId() == 0L || entry.getLogDate() == null) {
                throw new IllegalArgumentException("Log entry must have an id and log date to be inserted");
            }
            log.debug("Indexing log entry Id: {}, with logDate : {}, for docUUID: {}", entry.getId(),
                    entry.getLogDate(), entry.getDocUUID());
            documents.add(MongoDBAuditEntryWriter.asDocument(entry));
        }
        collection.insertMany(documents);
    }

    @Override
    public Long getEventsCount(String eventId) {
        return collection.countDocuments(Filters.eq("eventId", eventId));
    }

    private List<LogEntry> buildLogEntries(FindIterable<Document> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false)
                            .map(MongoDBAuditEntryReader::read)
                            .collect(Collectors.toList());
    }

    private void logRequest(Bson filter, Bson orderBy) {
        log.debug("MongoDB: FILTER {}{}", () -> filter, () -> orderBy == null ? "" : " ORDER BY " + orderBy);
    }

    private void logRequest(Bson filter, int pageNb, int pageSize) {
        log.debug("MongoDB: FILTER {} OFFSET {} LIMIT {}", filter, pageNb, pageSize);
    }

    @Override
    public void append(List<String> jsonEntries) {
        // we need to parse json with jackson first because Document#parse from mongodb driver will parse number as int
        List<Document> entries = new ArrayList<>();
        var renderingContext = RenderingContext.CtxBuilder.get();
        for (String json : jsonEntries) {
            try {
                var entry = MarshallerHelper.jsonToObject(LogEntry.class, json, renderingContext);
                if (entry.getId() == 0) {
                    throw new NuxeoException("A json entry has an empty id. entry=" + json);
                }
                Document doc = MongoDBAuditEntryWriter.asDocument(entry);
                entries.add(doc);
            } catch (IOException e) {
                throw new NuxeoException("Unable to deserialize json entry=" + json, e);
            }
        }
        collection.insertMany(entries);
    }

    @SuppressWarnings("resource") // CursorResult is being registered, must not be closed
    @Override
    public ScrollResult<String> scroll(QueryBuilder query, int batchSize, int keepAliveSeconds) {
        // create MongoDB filter & order
        var builder = new MongoDBQuerySearchBuilder(new MongoDBSearchConverter(LOG_ID), query);
        builder.walk();
        var filter = builder.getFilter();
        var sort = builder.getSort();

        logRequest(filter, sort);
        MongoCursor<Document> cursor = collection.find(filter).sort(sort).batchSize(batchSize).iterator();
        String scrollId = storageCursorService.registerCursor(cursor, batchSize, keepAliveSeconds);
        return scroll(scrollId);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return storageCursorService.scroll(scrollId);
    }

    @Override
    protected void clearEntries() {
        // clear audit
        collection.drop();
        // clear sequencer
        ((KeyValueStoreProvider) Framework.getService(KeyValueService.class)
                                          .getKeyValueStore(DEFAULT_STORE_NAME)).clear();
    }

    @Override
    public boolean hasCapability(Capability capability) {
        return switch (capability) {
            case EXTENDED_INFO_SEARCH -> true;
            case STARTS_WITH_PARTIAL_MATCH -> true;
        };
    }

    @Override
    public CursorService<MongoCursor<Document>, Document, String> getCursorService() {
        return cursorService;
    }
}
