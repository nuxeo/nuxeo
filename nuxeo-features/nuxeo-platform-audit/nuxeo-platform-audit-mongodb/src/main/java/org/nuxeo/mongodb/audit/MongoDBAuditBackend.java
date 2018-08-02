/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_PATH;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_DOC_UUID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_DATE;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_EVENT_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_ID;
import static org.nuxeo.runtime.mongodb.MongoDBSerializationHelper.MONGODB_ID;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.ecm.core.api.CursorService;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.query.sql.model.Literals;
import org.nuxeo.ecm.core.query.sql.model.MultiExpression;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Operator;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.OrderByList;
import org.nuxeo.ecm.core.query.sql.model.Predicate;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.uidgen.UIDGeneratorService;
import org.nuxeo.ecm.core.uidgen.UIDSequencer;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.audit.service.AbstractAuditBackend;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.BaseLogEntryProvider;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.audit.service.extension.AuditBackendDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.mongodb.MongoDBComponent;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.util.JSON;

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

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected MongoCollection<Document> collection;

    protected MongoDBLogEntryProvider provider = new MongoDBLogEntryProvider();

    protected CursorService<MongoCursor<Document>, Document, String> cursorService;

    public MongoDBAuditBackend(NXAuditEventsService component, AuditBackendDescriptor config) {
        super(component, config);
    }

    /**
     * @since 9.3
     */
    public MongoDBAuditBackend() {
        super();
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
        collection.createIndex(Indexes.ascending(LOG_DOC_UUID)); // query by doc id
        collection.createIndex(Indexes.ascending(LOG_EVENT_DATE)); // query by date range
        collection.createIndex(Indexes.ascending(LOG_EVENT_ID)); // query by type of event
        collection.createIndex(Indexes.ascending(LOG_DOC_PATH)); // query by path
        cursorService = new CursorService<>(doc -> {
            Object id = doc.remove(MONGODB_ID);
            if (id != null) {
                doc.put(LOG_ID, id);
            }
            return JSON.serialize(doc);
        });
    }

    @Override
    public void onApplicationStopped() {
        collection = null;
        cursorService.clear();
        cursorService = null;
    }

    /**
     * @return the {@link MongoCollection} configured with audit settings.
     */
    public MongoCollection<Document> getAuditCollection() {
        return collection;
    }

    @Override
    public List<LogEntry> queryLogs(QueryBuilder builder) {
        // prepare parameters
        Predicate predicate = builder.predicate();
        OrderByList orders = builder.orders();
        long offset = builder.offset();
        long limit = builder.limit();

        // create MongoDB filter
        Bson mgFilter = createFilter(predicate);

        // create MongoDB order
        Bson mgOrder = createSort(orders);

        logRequest(mgFilter, mgOrder);
        FindIterable<Document> iterable = collection.find(mgFilter).sort(mgOrder).skip((int) offset).limit((int) limit);
        return buildLogEntries(iterable);
    }

    protected Bson createFilter(Predicate andPredicate) {
        // cast parameters
        // current implementation only support a MultiExpression with AND operator
        @SuppressWarnings("unchecked")
        List<Predicate> predicates = (List<Predicate>) ((List<?>) ((MultiExpression) andPredicate).values);
        // current implementation only use Predicate/OrderByExpr with a simple Reference for left and right
        Function<Operand, String> getFieldName = operand -> ((Reference) operand).name;
        getFieldName = getFieldName.andThen(this::getMongoDBKey);

        List<Bson> filterList = new ArrayList<>(predicates.size());
        for (Predicate predicate : predicates) {
            String leftName = getFieldName.apply(predicate.lvalue);
            Operator operator = predicate.operator;
            Object rightValue = Literals.valueOf(predicate.rvalue);
            if (Operator.EQ.equals(operator)) {
                filterList.add(Filters.eq(leftName, rightValue));
            } else if (Operator.NOTEQ.equals(operator)) {
                filterList.add(Filters.ne(leftName, rightValue));
            } else if (Operator.LT.equals(operator)) {
                filterList.add(Filters.lt(leftName, predicate.rvalue));
            } else if (Operator.LTEQ.equals(operator)) {
                filterList.add(Filters.lte(leftName, rightValue));
            } else if (Operator.GTEQ.equals(operator)) {
                filterList.add(Filters.gte(leftName, rightValue));
            } else if (Operator.GT.equals(operator)) {
                filterList.add(Filters.gt(leftName, rightValue));
            } else if (Operator.IN.equals(operator)) {
                filterList.add(Filters.in(leftName, (List<?>) rightValue));
            } else if (Operator.STARTSWITH.equals(operator)) {
                filterList.add(Filters.regex(leftName, "^" + Pattern.quote(String.valueOf(rightValue))));
            }
        }
        return Filters.and(filterList);
    }

    protected Bson createSort(OrderByList orders) {
        List<Bson> orderList = new ArrayList<>(orders.size());
        for (OrderByExpr order : orders) {
            String name = getMongoDBKey(order.reference.name);
            if (order.isDescending) {
                orderList.add(Sorts.descending(name));
            } else {
                orderList.add(Sorts.ascending(name));
            }
        }
        return Sorts.orderBy(orderList);
    }

    protected String getMongoDBKey(String key) {
        if (LOG_ID.equals(key)) {
            return MONGODB_ID;
        }
        return key;
    }

    @Override
    public LogEntry getLogEntryByID(long id) {
        Document document = collection.find(Filters.eq(MONGODB_ID, Long.valueOf(id))).first();
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
                list.add(Filters.eq(LOG_EVENT_ID, eventIds[0]));
            } else {
                list.add(Filters.in(LOG_EVENT_ID, eventIds));
            }
        }
        if (categories != null && categories.length > 0) {
            if (categories.length == 1) {
                list.add(Filters.eq(LOG_CATEGORY, categories[0]));
            } else {
                list.add(Filters.in(LOG_CATEGORY, categories));
            }
        }
        if (path != null) {
            list.add(Filters.eq(LOG_DOC_PATH, path));
        }
        if (limit != null) {
            list.add(Filters.lt(LOG_EVENT_DATE, limit));
        }
        Bson filter = list.size() == 1 ? list.get(0) : Filters.and(list);
        logRequest(filter, pageNb, pageSize);
        FindIterable<Document> iterable = collection.find(filter).skip(pageNb * pageSize).limit(pageSize);
        return buildLogEntries(iterable);
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

    @Override
    public void append(List<String> jsonEntries) {
        // we need to parse json with jackson first because Document#parse from mongodb driver will parse number as int
        List<Document> entries = new ArrayList<>();
        for (String json : jsonEntries) {
            try {
                LogEntryImpl entry = OBJECT_MAPPER.readValue(json, LogEntryImpl.class);
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

    @Override
    public ScrollResult<String> scroll(QueryBuilder builder, int batchSize, int keepAliveSeconds) {
        // prepare parameters
        Predicate predicate = builder.predicate();
        OrderByList orders = builder.orders();

        // create MongoDB filter
        Bson mgFilter = createFilter(predicate);

        // create MongoDB order
        Bson mgOrder = createSort(orders);

        logRequest(mgFilter, mgOrder);
        MongoCursor<Document> cursor = collection.find(mgFilter).sort(mgOrder).batchSize(batchSize).iterator();
        String scrollId = cursorService.registerCursor(cursor, batchSize, keepAliveSeconds);
        return scroll(scrollId);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return cursorService.scroll(scrollId);
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

    }

}
