/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_STATUS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_USER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_BLOB_DATA;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_BINARY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_JOBID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_SIMPLE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_TRASHED;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LIFECYCLE_STATE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_CREATED;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_OWNER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_TARGET_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_VERSION_SERIES_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_READ_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_SERIES_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.resource.spi.ConnectionManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.CursorService;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.ecm.core.storage.dbs.DBSStateFlattener;
import org.nuxeo.ecm.core.storage.dbs.DBSTransactionState.ChangeTokenUpdater;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;

import com.mongodb.Block;
import com.mongodb.QueryOperators;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * MongoDB implementation of a {@link Repository}.
 *
 * @since 5.9.4
 */
public class MongoDBRepository extends DBSRepositoryBase {

    private static final Log log = LogFactory.getLog(MongoDBRepository.class);

    /**
     * Prefix used to retrieve a MongoDB connection from {@link MongoDBConnectionService}.
     * <p />
     * The connection id will be {@code repository/[REPOSITORY_NAME]}.
     */
    public static final String REPOSITORY_CONNECTION_PREFIX = "repository/";

    public static final Long LONG_ZERO = Long.valueOf(0);

    public static final Double ZERO = Double.valueOf(0);

    public static final Double ONE = Double.valueOf(1);

    public static final String MONGODB_ID = "_id";

    public static final String MONGODB_INC = "$inc";

    public static final String MONGODB_SET = "$set";

    public static final String MONGODB_UNSET = "$unset";

    public static final String MONGODB_PUSH = "$push";

    public static final String MONGODB_EACH = "$each";

    public static final String MONGODB_META = "$meta";

    public static final String MONGODB_TEXT_SCORE = "textScore";

    private static final String FULLTEXT_INDEX_NAME = "fulltext";

    private static final String LANGUAGE_FIELD = "__language";

    protected static final String COUNTER_NAME_UUID = "ecm:id";

    protected static final String COUNTER_FIELD = "seq";

    protected final MongoCollection<Document> coll;

    protected final MongoCollection<Document> countersColl;

    /** The key to use to store the id in the database. */
    protected String idKey;

    /** True if we don't use MongoDB's native "_id" key to store the id. */
    protected boolean useCustomId;

    /** Number of values still available in the in-memory sequence. */
    protected long sequenceLeft;

    /** Last value used from the in-memory sequence. */
    protected long sequenceLastValue;

    /** Sequence allocation block size. */
    protected long sequenceBlockSize;

    protected final MongoDBConverter converter;

    protected final CursorService<MongoCursor<Document>, Document, String> cursorService;

    public MongoDBRepository(ConnectionManager cm, MongoDBRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor);
        MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
        // prefix with repository/ to group repository connection
        MongoDatabase database = mongoService.getDatabase(REPOSITORY_CONNECTION_PREFIX + descriptor.name);
        coll = database.getCollection(descriptor.name);
        countersColl = database.getCollection(descriptor.name + ".counters");
        if (Boolean.TRUE.equals(descriptor.nativeId)) {
            idKey = MONGODB_ID;
        } else {
            idKey = KEY_ID;
        }
        useCustomId = KEY_ID.equals(idKey);
        if (idType == IdType.sequence || DEBUG_UUIDS) {
            Integer sbs = descriptor.sequenceBlockSize;
            sequenceBlockSize = sbs == null ? 1 : sbs.longValue();
            sequenceLeft = 0;
        }
        converter = new MongoDBConverter(idKey);
        cursorService = new CursorService<>(ob -> (String) ob.get(converter.keyToBson(KEY_ID)));
        initRepository();
    }

    @Override
    public List<IdType> getAllowedIdTypes() {
        return Arrays.asList(IdType.varchar, IdType.sequence);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        cursorService.clear();
    }

    protected void initRepository() {
        // create required indexes
        // code does explicit queries on those
        if (useCustomId) {
            coll.createIndex(Indexes.ascending(idKey));
        }
        coll.createIndex(Indexes.ascending(KEY_PARENT_ID));
        coll.createIndex(Indexes.ascending(KEY_ANCESTOR_IDS));
        coll.createIndex(Indexes.ascending(KEY_VERSION_SERIES_ID));
        coll.createIndex(Indexes.ascending(KEY_PROXY_TARGET_ID));
        coll.createIndex(Indexes.ascending(KEY_PROXY_VERSION_SERIES_ID));
        coll.createIndex(Indexes.ascending(KEY_READ_ACL));
        coll.createIndex(Indexes.ascending(KEY_PARENT_ID, KEY_NAME));
        // often used in user-generated queries
        coll.createIndex(Indexes.ascending(KEY_PRIMARY_TYPE));
        coll.createIndex(Indexes.ascending(KEY_LIFECYCLE_STATE));
        coll.createIndex(Indexes.ascending(KEY_IS_TRASHED));
        coll.createIndex(Indexes.ascending(KEY_FULLTEXT_JOBID));
        coll.createIndex(Indexes.ascending(KEY_ACP + "." + KEY_ACL + "." + KEY_ACE_USER));
        coll.createIndex(Indexes.ascending(KEY_ACP + "." + KEY_ACL + "." + KEY_ACE_STATUS));
        // TODO configure these from somewhere else
        coll.createIndex(Indexes.descending("dc:modified"));
        coll.createIndex(Indexes.ascending("rend:renditionName"));
        coll.createIndex(Indexes.ascending("drv:subscriptions.enabled"));
        coll.createIndex(Indexes.ascending("collectionMember:collectionIds"));
        coll.createIndex(Indexes.ascending("nxtag:tags"));
        if (!isFulltextSearchDisabled()) {
            Bson indexKeys = Indexes.compoundIndex( //
                    Indexes.text(KEY_FULLTEXT_SIMPLE), //
                    Indexes.text(KEY_FULLTEXT_BINARY) //
            );
            IndexOptions indexOptions = new IndexOptions().name(FULLTEXT_INDEX_NAME).languageOverride(LANGUAGE_FIELD);
            coll.createIndex(indexKeys, indexOptions);
        }
        // check root presence
        if (coll.count(Filters.eq(idKey, getRootId())) > 0) {
            return;
        }
        // create basic repository structure needed
        if (idType == IdType.sequence || DEBUG_UUIDS) {
            // create the id counter
            Document idCounter = new Document();
            idCounter.put(MONGODB_ID, COUNTER_NAME_UUID);
            idCounter.put(COUNTER_FIELD, LONG_ZERO);
            countersColl.insertOne(idCounter);
        }
        initRoot();
    }

    protected synchronized Long getNextSequenceId() {
        if (sequenceLeft == 0) {
            // allocate a new sequence block
            // the database contains the last value from the last block
            Bson filter = Filters.eq(MONGODB_ID, COUNTER_NAME_UUID);
            Bson update = Updates.inc(COUNTER_FIELD, Long.valueOf(sequenceBlockSize));
            Document idCounter = countersColl.findOneAndUpdate(filter, update,
                    new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
            if (idCounter == null) {
                throw new NuxeoException("Repository id counter not initialized");
            }
            sequenceLeft = sequenceBlockSize;
            sequenceLastValue = ((Long) idCounter.get(COUNTER_FIELD)).longValue() - sequenceBlockSize;
        }
        sequenceLeft--;
        sequenceLastValue++;
        return Long.valueOf(sequenceLastValue);
    }

    @Override
    public String generateNewId() {
        if (idType == IdType.sequence || DEBUG_UUIDS) {
            Long id = getNextSequenceId();
            if (DEBUG_UUIDS) {
                return "UUID_" + id;
            }
            return id.toString();
        } else {
            return UUID.randomUUID().toString();
        }
    }

    @Override
    public void createState(State state) {
        Document doc = converter.stateToBson(state);
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: CREATE " + doc.get(idKey) + ": " + doc);
        }
        coll.insertOne(doc);
        // TODO dupe exception
        // throw new DocumentException("Already exists: " + id);
    }

    @Override
    public void createStates(List<State> states) {
        List<Document> docs = states.stream().map(converter::stateToBson).collect(Collectors.toList());
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: CREATE ["
                    + docs.stream().map(doc -> doc.get(idKey).toString()).collect(Collectors.joining(", "))
                    + "]: " + docs);
        }
        coll.insertMany(docs);
    }

    @Override
    public State readState(String id) {
        return findOne(Filters.eq(idKey, id));
    }

    @Override
    public State readPartialState(String id, Collection<String> keys) {
        Document fields = new Document();
        keys.forEach(key -> fields.put(converter.keyToBson(key), ONE));
        return findOne(Filters.eq(idKey, id), fields);
    }

    @Override
    public List<State> readStates(List<String> ids) {
        return findAll(Filters.in(idKey, ids));
    }

    @Override
    public void updateState(String id, StateDiff diff, ChangeTokenUpdater changeTokenUpdater) {
        List<Document> updates = converter.diffToBson(diff);
        for (Document update : updates) {
            Document filter = new Document(idKey, id);
            if (changeTokenUpdater == null) {
                if (log.isTraceEnabled()) {
                    log.trace("MongoDB: UPDATE " + id + ": " + update);
                }
            } else {
                // assume bson is identical to dbs internals
                // condition works even if value is null
                Map<String, Serializable> conditions = changeTokenUpdater.getConditions();
                Map<String, Serializable> tokenUpdates = changeTokenUpdater.getUpdates();
                if (update.containsKey(MONGODB_SET)) {
                    ((Document) update.get(MONGODB_SET)).putAll(tokenUpdates);
                } else {
                    Document set = new Document();
                    set.putAll(tokenUpdates);
                    update.put(MONGODB_SET, set);
                }
                if (log.isTraceEnabled()) {
                    log.trace("MongoDB: UPDATE " + id + ": IF " + conditions + " THEN " + update);
                }
                filter.putAll(conditions);
            }
            UpdateResult w = coll.updateMany(filter, update);
            if (w.getModifiedCount() != 1) {
                log.trace("MongoDB:    -> CONCURRENT UPDATE: " + id);
                throw new ConcurrentUpdateException(id);
            }
            // TODO dupe exception
            // throw new DocumentException("Missing: " + id);
        }
    }

    @Override
    public void deleteStates(Set<String> ids) {
        Bson filter = Filters.in(idKey, ids);
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: REMOVE " + ids);
        }
        DeleteResult w = coll.deleteMany(filter);
        if (w.getDeletedCount() != ids.size()) {
            if (log.isDebugEnabled()) {
                log.debug("Removed " + w.getDeletedCount() + " docs for " + ids.size() + " ids: " + ids);
            }
        }
    }

    @Override
    public State readChildState(String parentId, String name, Set<String> ignored) {
        Bson filter = getChildQuery(parentId, name, ignored);
        return findOne(filter);
    }

    protected void logQuery(String id, Bson fields) {
        logQuery(Filters.eq(idKey, id), fields);
    }

    protected void logQuery(Bson filter, Bson fields) {
        if (fields == null) {
            log.trace("MongoDB: QUERY " + filter);
        } else {
            log.trace("MongoDB: QUERY " + filter + " KEYS " + fields);
        }
    }

    protected void logQuery(Bson query, Bson fields, Bson orderBy, int limit, int offset) {
        log.trace("MongoDB: QUERY " + query + " KEYS " + fields + (orderBy == null ? "" : " ORDER BY " + orderBy)
                + " OFFSET " + offset + " LIMIT " + limit);
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        Document filter = getChildQuery(parentId, name, ignored);
        return exists(filter);
    }

    protected Document getChildQuery(String parentId, String name, Set<String> ignored) {
        Document filter = new Document();
        filter.put(KEY_PARENT_ID, parentId);
        filter.put(KEY_NAME, name);
        addIgnoredIds(filter, ignored);
        return filter;
    }

    protected void addIgnoredIds(Document filter, Set<String> ignored) {
        if (!ignored.isEmpty()) {
            Document notInIds = new Document(QueryOperators.NIN, new ArrayList<>(ignored));
            filter.put(idKey, notInIds);
        }
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        Document filter = new Document(converter.keyToBson(key), value);
        addIgnoredIds(filter, ignored);
        return findAll(filter);
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        Document filter = new Document(converter.keyToBson(key1), value1);
        filter.put(converter.keyToBson(key2), value2);
        addIgnoredIds(filter, ignored);
        return findAll(filter);
    }

    @Override
    public Stream<State> getDescendants(String rootId, Set<String> keys) {
        return getDescendants(rootId, keys, 0);
    }

    @Override
    public Stream<State> getDescendants(String rootId, Set<String> keys, int limit) {
        Bson filter = Filters.eq(KEY_ANCESTOR_IDS, rootId);
        Document fields = new Document();
        if (useCustomId) {
            fields.put(MONGODB_ID, ZERO);
        }
        fields.put(idKey, ONE);
        keys.forEach(key -> fields.put(converter.keyToBson(key), ONE));
        return stream(filter, fields, limit);
    }

    @Override
    public boolean queryKeyValuePresence(String key, String value, Set<String> ignored) {
        Document filter = new Document(converter.keyToBson(key), value);
        addIgnoredIds(filter, ignored);
        return exists(filter);
    }

    protected boolean exists(Bson filter) {
        return exists(filter, justPresenceField());
    }

    protected boolean exists(Bson filter, Bson projection) {
        if (log.isTraceEnabled()) {
            logQuery(filter, projection);
        }
        return coll.find(filter).projection(projection).first() != null;
    }

    protected State findOne(Bson filter) {
        return findOne(filter, null);
    }

    protected State findOne(Bson filter, Bson projection) {
        try (Stream<State> stream = stream(filter, projection)) {
            return stream.findAny().orElse(null);
        }
    }

    protected List<State> findAll(Bson filter) {
        try (Stream<State> stream = stream(filter)) {
            return stream.collect(Collectors.toList());
        }
    }

    protected Stream<State> stream(Bson filter) {
        return stream(filter, null, 0);
    }

    protected Stream<State> stream(Bson filter, Bson projection) {
        return stream(filter, projection, 0);
    }

    /**
     * Logs, runs request and constructs a closeable {@link Stream} on top of {@link MongoCursor}.
     * <p />
     * We should rely on this method, because it correctly handles cursor closed state.
     * <p />
     * Note: Looping on {@link FindIterable} or {@link MongoIterable} could lead to cursor leaks. This is also the case
     * on some call to {@link MongoIterable#first()}.
     *
     * @return a closeable {@link Stream} instance linked to {@link MongoCursor}
     */
    protected Stream<State> stream(Bson filter, Bson projection, int limit) {
        if (filter == null) {
            // empty filter
            filter = new Document();
        }
        // it's ok if projection is null
        if (log.isTraceEnabled()) {
            logQuery(filter, projection);
        }

        boolean completedAbruptly = true;
        MongoCursor<Document> cursor = coll.find(filter).limit(limit).projection(projection).iterator();
        try {
            Set<String> seen = new HashSet<>();
            Stream<State> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, 0), false) //
                                                .onClose(cursor::close)
                                                .filter(doc -> seen.add(doc.getString(idKey)))
                                                // MongoDB cursors may return the same
                                                // object several times
                                                .map(converter::bsonToState);
            // the stream takes responsibility for closing the session
            completedAbruptly = false;
            return stream;
        } finally {
            if (completedAbruptly) {
                cursor.close();
            }
        }
    }

    protected Document justPresenceField() {
        return new Document(MONGODB_ID, ONE);
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, boolean distinctDocuments, int limit, int offset, int countUpTo) {
        // orderByClause may be null and different from evaluator.getOrderByClause() in case we want to post-filter
        MongoDBQueryBuilder builder = new MongoDBQueryBuilder(this, evaluator.getExpression(),
                evaluator.getSelectClause(), orderByClause, evaluator.pathResolver, evaluator.fulltextSearchDisabled);
        builder.walk();
        if (builder.hasFulltext && isFulltextSearchDisabled()) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        Document filter = builder.getQuery();
        addPrincipals(filter, evaluator.principals);
        Bson orderBy = builder.getOrderBy();
        Bson keys = builder.getProjection();
        // Don't do manual projection if there are no projection wildcards, as this brings no new
        // information and is costly. The only difference is several identical rows instead of one.
        boolean manualProjection = !distinctDocuments && builder.hasProjectionWildcard();
        if (manualProjection) {
            // we'll do post-treatment to re-evaluate the query to get proper wildcard projections
            // so we need the full state from the database
            keys = null;
            evaluator.parse();
        }

        if (log.isTraceEnabled()) {
            logQuery(filter, keys, orderBy, limit, offset);
        }

        List<Map<String, Serializable>> projections;
        long totalSize;
        try (MongoCursor<Document> cursor = coll.find(filter)
                                                .projection(keys)
                                                .skip(offset)
                                                .limit(limit)
                                                .sort(orderBy)
                                                .iterator()) {
            projections = new ArrayList<>();
            DBSStateFlattener flattener = new DBSStateFlattener(builder.propertyKeys);
            Iterable<Document> docs = () -> cursor;
            for (Document doc : docs) {
                State state = converter.bsonToState(doc);
                if (manualProjection) {
                    projections.addAll(evaluator.matches(state));
                } else {
                    projections.add(flattener.flatten(state));
                }
            }
        }
        if (countUpTo == -1) {
            // count full size
            if (limit == 0) {
                totalSize = projections.size();
            } else if (manualProjection) {
                totalSize = -1; // unknown due to manual projection
            } else {
                totalSize = coll.count(filter);
            }
        } else if (countUpTo == 0) {
            // no count
            totalSize = -1; // not counted
        } else {
            // count only if less than countUpTo
            if (limit == 0) {
                totalSize = projections.size();
            } else if (manualProjection) {
                totalSize = -1; // unknown due to manual projection
            } else {
                totalSize = coll.count(filter, new CountOptions().limit(countUpTo + 1));
            }
            if (totalSize > countUpTo) {
                totalSize = -2; // truncated
            }
        }
        if (log.isTraceEnabled() && projections.size() != 0) {
            log.trace("MongoDB:    -> " + projections.size());
        }
        return new PartialList<>(projections, totalSize);
    }

    @Override
    public ScrollResult<String> scroll(DBSExpressionEvaluator evaluator, int batchSize, int keepAliveSeconds) {
        cursorService.checkForTimedOutScroll();
        MongoDBQueryBuilder builder = new MongoDBQueryBuilder(this, evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled);
        builder.walk();
        if (builder.hasFulltext && isFulltextSearchDisabled()) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        Bson filter = builder.getQuery();
        Bson keys = builder.getProjection();
        if (log.isTraceEnabled()) {
            logQuery(filter, keys, null, 0, 0);
        }

        MongoCursor<Document> cursor = coll.find(filter).projection(keys).batchSize(batchSize).iterator();
        String scrollId = cursorService.registerCursor(cursor, batchSize, keepAliveSeconds);
        return scroll(scrollId);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return cursorService.scroll(scrollId);
    }

    protected void addPrincipals(Document query, Set<String> principals) {
        if (principals != null) {
            Document inPrincipals = new Document(QueryOperators.IN, new ArrayList<>(principals));
            query.put(DBSDocument.KEY_READ_ACL, inPrincipals);
        }
    }

    /** Keys used for document projection when marking all binaries for GC. */
    protected Bson binaryKeys;

    @Override
    protected void initBlobsPaths() {
        MongoDBBlobFinder finder = new MongoDBBlobFinder();
        finder.visit();
        binaryKeys = Projections.fields(finder.binaryKeys);
    }

    protected static class MongoDBBlobFinder extends BlobFinder {
        protected List<Bson> binaryKeys = new ArrayList<>(Collections.singleton(Projections.excludeId()));

        @Override
        protected void recordBlobPath() {
            path.addLast(KEY_BLOB_DATA);
            binaryKeys.add(Projections.include(StringUtils.join(path, ".")));
            path.removeLast();
        }
    }

    @Override
    public void markReferencedBinaries() {
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        // TODO add a query to not scan all documents
        if (log.isTraceEnabled()) {
            logQuery(new Document(), binaryKeys);
        }
        Block<Document> block = doc -> markReferencedBinaries(doc, blobManager);
        coll.find().projection(binaryKeys).forEach(block);
    }

    protected void markReferencedBinaries(Document ob, DocumentBlobManager blobManager) {
        for (String key : ob.keySet()) {
            Object value = ob.get(key);
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                for (Object v : list) {
                    if (v instanceof Document) {
                        markReferencedBinaries((Document) v, blobManager);
                    } else {
                        markReferencedBinary(v, blobManager);
                    }
                }
            } else if (value instanceof Object[]) {
                for (Object v : (Object[]) value) {
                    markReferencedBinary(v, blobManager);
                }
            } else if (value instanceof Document) {
                markReferencedBinaries((Document) value, blobManager);
            } else {
                markReferencedBinary(value, blobManager);
            }
        }
    }

    protected void markReferencedBinary(Object value, DocumentBlobManager blobManager) {
        if (!(value instanceof String)) {
            return;
        }
        String key = (String) value;
        blobManager.markReferencedBinary(key, repositoryName);
    }

    protected static final Bson LOCK_FIELDS = Projections.include(KEY_LOCK_OWNER, KEY_LOCK_CREATED);

    protected static final Bson UNSET_LOCK_UPDATE = Updates.combine(Updates.unset(KEY_LOCK_OWNER),
            Updates.unset(KEY_LOCK_CREATED));

    @Override
    public Lock getLock(String id) {
        if (log.isTraceEnabled()) {
            logQuery(id, LOCK_FIELDS);
        }
        Document res = coll.find(Filters.eq(idKey, id)).projection(LOCK_FIELDS).first();
        if (res == null) {
            // document not found
            throw new DocumentNotFoundException(id);
        }
        String owner = res.getString(KEY_LOCK_OWNER);
        if (owner == null) {
            // not locked
            return null;
        }
        Calendar created = (Calendar) converter.scalarToSerializable(res.get(KEY_LOCK_CREATED));
        return new Lock(owner, created);
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        Bson filter = Filters.and( //
                Filters.eq(idKey, id), //
                Filters.exists(KEY_LOCK_OWNER, false) // select doc if no lock is set
        );
        Bson setLock = Updates.combine( //
                Updates.set(KEY_LOCK_OWNER, lock.getOwner()), //
                Updates.set(KEY_LOCK_CREATED, converter.serializableToBson(lock.getCreated())) //
        );
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: FINDANDMODIFY " + filter + " UPDATE " + setLock);
        }
        Document res = coll.findOneAndUpdate(filter, setLock);
        if (res != null) {
            // found a doc to lock
            return null;
        } else {
            // doc not found, or lock owner already set
            // get the old lock
            if (log.isTraceEnabled()) {
                logQuery(id, LOCK_FIELDS);
            }
            Document old = coll.find(Filters.eq(idKey, id)).projection(LOCK_FIELDS).first();
            if (old == null) {
                // document not found
                throw new DocumentNotFoundException(id);
            }
            String oldOwner = (String) old.get(KEY_LOCK_OWNER);
            Calendar oldCreated = (Calendar) converter.scalarToSerializable(old.get(KEY_LOCK_CREATED));
            if (oldOwner != null) {
                return new Lock(oldOwner, oldCreated);
            }
            // no lock -- there was a race condition
            // TODO do better
            throw new ConcurrentUpdateException("Lock " + id);
        }
    }

    @Override
    public Lock removeLock(String id, String owner) {
        Document filter = new Document(idKey, id);
        if (owner != null) {
            // remove if owner matches or null
            // implements LockManager.canLockBeRemoved inside MongoDB
            Object ownerOrNull = Arrays.asList(owner, null);
            filter.put(KEY_LOCK_OWNER, new Document(QueryOperators.IN, ownerOrNull));
        } // else unconditional remove
        // remove the lock
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: FINDANDMODIFY " + filter + " UPDATE " + UNSET_LOCK_UPDATE);
        }
        Document old = coll.findOneAndUpdate(filter, UNSET_LOCK_UPDATE);
        if (old != null) {
            // found a doc and removed the lock, return previous lock
            String oldOwner = (String) old.get(KEY_LOCK_OWNER);
            if (oldOwner == null) {
                // was not locked
                return null;
            } else {
                // return previous lock
                Calendar oldCreated = (Calendar) converter.scalarToSerializable(old.get(KEY_LOCK_CREATED));
                return new Lock(oldOwner, oldCreated);
            }
        } else {
            // doc not found, or lock owner didn't match
            // get the old lock
            if (log.isTraceEnabled()) {
                logQuery(id, LOCK_FIELDS);
            }
            old = coll.find(Filters.eq(idKey, id)).projection(LOCK_FIELDS).first();
            if (old == null) {
                // document not found
                throw new DocumentNotFoundException(id);
            }
            String oldOwner = (String) old.get(KEY_LOCK_OWNER);
            Calendar oldCreated = (Calendar) converter.scalarToSerializable(old.get(KEY_LOCK_CREATED));
            if (oldOwner != null) {
                if (!LockManager.canLockBeRemoved(oldOwner, owner)) {
                    // existing mismatched lock, flag failure
                    return new Lock(oldOwner, oldCreated, true);
                }
                // old owner should have matched -- there was a race condition
                // TODO do better
                throw new ConcurrentUpdateException("Unlock " + id);
            }
            // old owner null, should have matched -- there was a race condition
            // TODO do better
            throw new ConcurrentUpdateException("Unlock " + id);
        }
    }

    @Override
    public void closeLockManager() {

    }

    @Override
    public void clearLockManagerCaches() {
    }

}
