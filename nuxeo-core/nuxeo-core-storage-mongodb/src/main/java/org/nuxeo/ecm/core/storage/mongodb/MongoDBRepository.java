/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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

import static java.lang.Boolean.TRUE;
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
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_PROXY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LIFECYCLE_STATE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_CREATED;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_OWNER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PRIMARY_TYPE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_TARGET_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_VERSION_SERIES_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_READ_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_SERIES_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.resource.spi.ConnectionManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.runtime.mongodb.MongoDBConnectionHelper;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.QueryOperators;
import com.mongodb.WriteResult;

/**
 * MongoDB implementation of a {@link Repository}.
 *
 * @since 5.9.4
 */
public class MongoDBRepository extends DBSRepositoryBase {

    private static final Log log = LogFactory.getLog(MongoDBRepository.class);

    public static final Long LONG_ZERO = Long.valueOf(0);

    public static final Double ZERO = Double.valueOf(0);

    public static final Double ONE = Double.valueOf(1);

    public static final Double MINUS_ONE = Double.valueOf(-1);

    public static final String DB_DEFAULT = "nuxeo";

    public static final String MONGODB_ID = "_id";

    public static final String MONGODB_INC = "$inc";

    public static final String MONGODB_SET = "$set";

    public static final String MONGODB_UNSET = "$unset";

    public static final String MONGODB_PUSH = "$push";

    public static final String MONGODB_EACH = "$each";

    public static final String MONGODB_META = "$meta";

    public static final String MONGODB_TEXT_SCORE = "textScore";

    private static final String MONGODB_INDEX_TEXT = "text";

    private static final String MONGODB_INDEX_NAME = "name";

    private static final String MONGODB_LANGUAGE_OVERRIDE = "language_override";

    private static final String FULLTEXT_INDEX_NAME = "fulltext";

    private static final String LANGUAGE_FIELD = "__language";

    protected static final String COUNTER_NAME_UUID = "ecm:id";

    protected static final String COUNTER_FIELD = "seq";

    protected MongoClient mongoClient;

    protected DBCollection coll;

    protected DBCollection countersColl;

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

    protected final CursorService<DBCursor, DBObject> cursorService = new CursorService<>();

    public MongoDBRepository(ConnectionManager cm, MongoDBRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor);
        mongoClient = MongoDBConnectionHelper.newMongoClient(descriptor.server);
        coll = getCollection(descriptor, mongoClient);
        countersColl = getCountersCollection(descriptor, mongoClient);
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
        mongoClient.close();
    }

    protected static DBCollection getCollection(MongoClient mongoClient, String dbname, String collection) {
        if (StringUtils.isBlank(dbname)) {
            dbname = DB_DEFAULT;
        }
        DB db = mongoClient.getDB(dbname);
        return db.getCollection(collection);
    }

    // used also by unit tests
    public static DBCollection getCollection(MongoDBRepositoryDescriptor descriptor, MongoClient mongoClient) {
        return getCollection(mongoClient, descriptor.dbname, descriptor.name);
    }

    // used also by unit tests
    public static DBCollection getCountersCollection(MongoDBRepositoryDescriptor descriptor, MongoClient mongoClient) {
        return getCollection(mongoClient, descriptor.dbname, descriptor.name + ".counters");
    }

    protected void initRepository() {
        // create required indexes
        // code does explicit queries on those
        if (useCustomId) {
            coll.createIndex(new BasicDBObject(idKey, ONE));
        }
        coll.createIndex(new BasicDBObject(KEY_PARENT_ID, ONE));
        coll.createIndex(new BasicDBObject(KEY_ANCESTOR_IDS, ONE));
        coll.createIndex(new BasicDBObject(KEY_VERSION_SERIES_ID, ONE));
        coll.createIndex(new BasicDBObject(KEY_PROXY_TARGET_ID, ONE));
        coll.createIndex(new BasicDBObject(KEY_PROXY_VERSION_SERIES_ID, ONE));
        coll.createIndex(new BasicDBObject(KEY_READ_ACL, ONE));
        DBObject parentChild = new BasicDBObject();
        parentChild.put(KEY_PARENT_ID, ONE);
        parentChild.put(KEY_NAME, ONE);
        coll.createIndex(parentChild);
        // often used in user-generated queries
        coll.createIndex(new BasicDBObject(KEY_PRIMARY_TYPE, ONE));
        coll.createIndex(new BasicDBObject(KEY_LIFECYCLE_STATE, ONE));
        coll.createIndex(new BasicDBObject(KEY_FULLTEXT_JOBID, ONE));
        coll.createIndex(new BasicDBObject(KEY_ACP + "." + KEY_ACL + "." + KEY_ACE_USER, ONE));
        coll.createIndex(new BasicDBObject(KEY_ACP + "." + KEY_ACL + "." + KEY_ACE_STATUS, ONE));
        // TODO configure these from somewhere else
        coll.createIndex(new BasicDBObject("dc:modified", MINUS_ONE));
        coll.createIndex(new BasicDBObject("rend:renditionName", ONE));
        coll.createIndex(new BasicDBObject("drv:subscriptions.enabled", ONE));
        coll.createIndex(new BasicDBObject("collectionMember:collectionIds", ONE));
        coll.createIndex(new BasicDBObject("nxtag:tags", ONE));
        if (!isFulltextDisabled()) {
            DBObject indexKeys = new BasicDBObject();
            indexKeys.put(KEY_FULLTEXT_SIMPLE, MONGODB_INDEX_TEXT);
            indexKeys.put(KEY_FULLTEXT_BINARY, MONGODB_INDEX_TEXT);
            DBObject indexOptions = new BasicDBObject();
            indexOptions.put(MONGODB_INDEX_NAME, FULLTEXT_INDEX_NAME);
            indexOptions.put(MONGODB_LANGUAGE_OVERRIDE, LANGUAGE_FIELD);
            coll.createIndex(indexKeys, indexOptions);
        }
        // check root presence
        DBObject query = new BasicDBObject(idKey, getRootId());
        if (coll.findOne(query, justPresenceField()) != null) {
            return;
        }
        // create basic repository structure needed
        if (idType == IdType.sequence || DEBUG_UUIDS) {
            // create the id counter
            DBObject idCounter = new BasicDBObject();
            idCounter.put(MONGODB_ID, COUNTER_NAME_UUID);
            idCounter.put(COUNTER_FIELD, LONG_ZERO);
            countersColl.insert(idCounter);
        }
        initRoot();
    }

    protected synchronized Long getNextSequenceId() {
        if (sequenceLeft == 0) {
            // allocate a new sequence block
            // the database contains the last value from the last block
            DBObject query = new BasicDBObject(MONGODB_ID, COUNTER_NAME_UUID);
            DBObject update = new BasicDBObject(MONGODB_INC,
                    new BasicDBObject(COUNTER_FIELD, Long.valueOf(sequenceBlockSize)));
            DBObject idCounter = countersColl.findAndModify(query, null, null, false, update, true, false);
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
        DBObject ob = converter.stateToBson(state);
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: CREATE " + ob.get(idKey) + ": " + ob);
        }
        coll.insert(ob);
        // TODO dupe exception
        // throw new DocumentException("Already exists: " + id);
    }

    @Override
    public void createStates(List<State> states) {
        List<DBObject> obs = states.stream().map(converter::stateToBson).collect(Collectors.toList());
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: CREATE ["
                    + obs.stream().map(ob -> ob.get(idKey).toString()).collect(Collectors.joining(", "))
                    + "]: " + obs);
        }
        coll.insert(obs);
    }

    @Override
    public State readState(String id) {
        DBObject query = new BasicDBObject(idKey, id);
        return findOne(query);
    }

    @Override
    public List<State> readStates(List<String> ids) {
        DBObject query = new BasicDBObject(idKey, new BasicDBObject(QueryOperators.IN, ids));
        return findAll(query, ids.size());
    }

    @Override
    public void updateState(String id, StateDiff diff, ChangeTokenUpdater changeTokenUpdater) {
        List<DBObject> updates = converter.diffToBson(diff);
        for (DBObject update : updates) {
            DBObject query = new BasicDBObject(idKey, id);
            if (changeTokenUpdater == null) {
                if (log.isTraceEnabled()) {
                    log.trace("MongoDB: UPDATE " + id + ": " + update);
                }
            } else {
                // assume bson is identical to dbs internals
                // condition works even if value is null
                Map<String, Serializable> conditions = changeTokenUpdater.getConditions();
                Map<String, Serializable> tokenUpdates = changeTokenUpdater.getUpdates();
                if (update.containsField(MONGODB_SET)) {
                    ((DBObject) update.get(MONGODB_SET)).putAll(tokenUpdates);
                } else {
                    DBObject set = new BasicDBObject();
                    set.putAll(tokenUpdates);
                    update.put(MONGODB_SET, set);
                }
                if (log.isTraceEnabled()) {
                    log.trace("MongoDB: UPDATE " + id + ": IF " + conditions + " THEN " + update);
                }
                query.putAll(conditions);
            }
            WriteResult w = coll.update(query, update);
            if (w.getN() != 1) {
                log.trace("MongoDB:    -> CONCURRENT UPDATE: " + id);
                throw new ConcurrentUpdateException(id);
            }
            // TODO dupe exception
            // throw new DocumentException("Missing: " + id);
        }
    }

    @Override
    public void deleteStates(Set<String> ids) {
        DBObject query = new BasicDBObject(idKey, new BasicDBObject(QueryOperators.IN, ids));
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: REMOVE " + ids);
        }
        WriteResult w = coll.remove(query);
        if (w.getN() != ids.size()) {
            log.error("Removed " + w.getN() + " docs for " + ids.size() + " ids: " + ids);
        }
    }

    @Override
    public State readChildState(String parentId, String name, Set<String> ignored) {
        DBObject query = getChildQuery(parentId, name, ignored);
        return findOne(query);
    }

    protected void logQuery(String id, DBObject fields) {
        logQuery(new BasicDBObject(idKey, id), fields);
    }

    protected void logQuery(DBObject query, DBObject fields) {
        if (fields == null) {
            log.trace("MongoDB: QUERY " + query);
        } else {
            log.trace("MongoDB: QUERY " + query + " KEYS " + fields);
        }
    }

    protected void logQuery(DBObject query, DBObject fields, DBObject orderBy, int limit, int offset) {
        log.trace("MongoDB: QUERY " + query + " KEYS " + fields + (orderBy == null ? "" : " ORDER BY " + orderBy)
                + " OFFSET " + offset + " LIMIT " + limit);
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        DBObject query = getChildQuery(parentId, name, ignored);
        if (log.isTraceEnabled()) {
            logQuery(query, justPresenceField());
        }
        return coll.findOne(query, justPresenceField()) != null;
    }

    protected DBObject getChildQuery(String parentId, String name, Set<String> ignored) {
        DBObject query = new BasicDBObject();
        query.put(KEY_PARENT_ID, parentId);
        query.put(KEY_NAME, name);
        addIgnoredIds(query, ignored);
        return query;
    }

    protected void addIgnoredIds(DBObject query, Set<String> ignored) {
        if (!ignored.isEmpty()) {
            DBObject notInIds = new BasicDBObject(QueryOperators.NIN, new ArrayList<>(ignored));
            query.put(idKey, notInIds);
        }
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        DBObject query = new BasicDBObject(converter.keyToBson(key), value);
        addIgnoredIds(query, ignored);
        return findAll(query, 0);
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        DBObject query = new BasicDBObject(converter.keyToBson(key1), value1);
        query.put(converter.keyToBson(key2), value2);
        addIgnoredIds(query, ignored);
        return findAll(query, 0);
    }

    @Override
    public void queryKeyValueArray(String key, Object value, Set<String> ids, Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies) {
        DBObject query = new BasicDBObject(key, value);
        DBObject fields = new BasicDBObject();
        if (useCustomId) {
            fields.put(MONGODB_ID, ZERO);
        }
        fields.put(idKey, ONE);
        fields.put(KEY_IS_PROXY, ONE);
        fields.put(KEY_PROXY_TARGET_ID, ONE);
        fields.put(KEY_PROXY_IDS, ONE);
        if (log.isTraceEnabled()) {
            logQuery(query, fields);
        }
        try (DBCursor cursor = coll.find(query, fields)) {
            for (DBObject ob : cursor) {
                String id = (String) ob.get(idKey);
                ids.add(id);
                if (proxyTargets != null && TRUE.equals(ob.get(KEY_IS_PROXY))) {
                    String targetId = (String) ob.get(KEY_PROXY_TARGET_ID);
                    proxyTargets.put(id, targetId);
                }
                if (targetProxies != null) {
                    Object[] proxyIds = (Object[]) converter.bsonToValue(ob.get(KEY_PROXY_IDS));
                    if (proxyIds != null) {
                        targetProxies.put(id, proxyIds);
                    }
                }
            }
        }
    }

    @Override
    public boolean queryKeyValuePresence(String key, String value, Set<String> ignored) {
        DBObject query = new BasicDBObject(key, value);
        addIgnoredIds(query, ignored);
        if (log.isTraceEnabled()) {
            logQuery(query, justPresenceField());
        }
        return coll.findOne(query, justPresenceField()) != null;
    }

    protected State findOne(DBObject query) {
        if (log.isTraceEnabled()) {
            logQuery(query, null);
        }
        return converter.bsonToState(coll.findOne(query));
    }

    protected List<State> findAll(DBObject query, int sizeHint) {
        if (log.isTraceEnabled()) {
            logQuery(query, null);
        }
        Set<String> seen = new HashSet<>();
        try (DBCursor cursor = coll.find(query)) {
            List<State> list = new ArrayList<>(sizeHint);
            for (DBObject ob : cursor) {
                if (!seen.add((String) ob.get(idKey))) {
                    // MongoDB cursors may return the same
                    // object several times
                    continue;
                }
                list.add(converter.bsonToState(ob));
            }
            return list;
        }
    }

    protected DBObject justPresenceField() {
        return new BasicDBObject(MONGODB_ID, ONE);
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, boolean distinctDocuments, int limit, int offset, int countUpTo) {
        // orderByClause may be null and different from evaluator.getOrderByClause() in case we want to post-filter
        MongoDBQueryBuilder builder = new MongoDBQueryBuilder(this, evaluator.getExpression(),
                evaluator.getSelectClause(), orderByClause, evaluator.pathResolver, evaluator.fulltextSearchDisabled);
        builder.walk();
        if (builder.hasFulltext && isFulltextDisabled()) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        DBObject query = builder.getQuery();
        addPrincipals(query, evaluator.principals);
        DBObject orderBy = builder.getOrderBy();
        DBObject keys = builder.getProjection();
        // Don't do manual projection if there are no projection wildcards, as this brings no new
        // information and is costly. The only difference is several identical rows instead of one.
        boolean manualProjection = !distinctDocuments && builder.hasProjectionWildcard();
        if (manualProjection) {
            // we'll do post-treatment to re-evaluate the query to get proper wildcard projections
            // so we need the full state from the database
            keys = new BasicDBObject();
            evaluator.parse();
        }

        if (log.isTraceEnabled()) {
            logQuery(query, keys, orderBy, limit, offset);
        }

        List<Map<String, Serializable>> projections;
        long totalSize;
        try (DBCursor cursor = coll.find(query, keys).skip(offset).limit(limit)) {
            if (orderBy != null) {
                cursor.sort(orderBy);
            }
            projections = new ArrayList<>();
            for (DBObject ob : cursor) {
                State state = converter.bsonToState(ob);
                if (manualProjection) {
                    projections.addAll(evaluator.matches(state));
                } else {
                    projections.add(DBSStateFlattener.flatten(state));
                }
            }
            if (countUpTo == -1) {
                // count full size
                if (limit == 0) {
                    totalSize = projections.size();
                } else {
                    totalSize = cursor.count();
                }
            } else if (countUpTo == 0) {
                // no count
                totalSize = -1; // not counted
            } else {
                // count only if less than countUpTo
                if (limit == 0) {
                    totalSize = projections.size();
                } else {
                    totalSize = cursor.copy().limit(countUpTo + 1).count();
                }
                if (totalSize > countUpTo) {
                    totalSize = -2; // truncated
                }
            }
        }
        if (log.isTraceEnabled() && projections.size() != 0) {
            log.trace("MongoDB:    -> " + projections.size());
        }
        return new PartialList<>(projections, totalSize);
    }

    @Override
    public ScrollResult scroll(DBSExpressionEvaluator evaluator, int batchSize, int keepAliveSeconds) {
        cursorService.checkForTimedOutScroll();
        MongoDBQueryBuilder builder = new MongoDBQueryBuilder(this, evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled);
        builder.walk();
        if (builder.hasFulltext && isFulltextDisabled()) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        DBObject query = builder.getQuery();
        DBObject keys = builder.getProjection();
        if (log.isTraceEnabled()) {
            logQuery(query, keys, null, 0, 0);
        }

        DBCursor cursor = coll.find(query, keys);
        String scrollId = cursorService.registerCursor(cursor, batchSize, keepAliveSeconds);
        return scroll(scrollId);
    }

    @Override
    public ScrollResult scroll(String scrollId) {
        return cursorService.scroll(scrollId, ob -> (String) ob.get(converter.keyToBson(KEY_ID)));
    }

    protected void addPrincipals(DBObject query, Set<String> principals) {
        if (principals != null) {
            DBObject inPrincipals = new BasicDBObject(QueryOperators.IN, new ArrayList<>(principals));
            query.put(DBSDocument.KEY_READ_ACL, inPrincipals);
        }
    }

    /** Keys used for document projection when marking all binaries for GC. */
    protected DBObject binaryKeys;

    @Override
    protected void initBlobsPaths() {
        MongoDBBlobFinder finder = new MongoDBBlobFinder();
        finder.visit();
        binaryKeys = finder.binaryKeys;
    }

    protected static class MongoDBBlobFinder extends BlobFinder {
        protected DBObject binaryKeys = new BasicDBObject(MONGODB_ID, ZERO);

        @Override
        protected void recordBlobPath() {
            path.addLast(KEY_BLOB_DATA);
            binaryKeys.put(StringUtils.join(path, "."), ONE);
            path.removeLast();
        }
    }

    @Override
    public void markReferencedBinaries() {
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        // TODO add a query to not scan all documents
        if (log.isTraceEnabled()) {
            logQuery(new BasicDBObject(), binaryKeys);
        }
        try (DBCursor cursor = coll.find(new BasicDBObject(), binaryKeys)) {
            for (DBObject ob : cursor) {
                markReferencedBinaries(ob, blobManager);
            }
        }
    }

    protected void markReferencedBinaries(DBObject ob, DocumentBlobManager blobManager) {
        for (String key : ob.keySet()) {
            Object value = ob.get(key);
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                for (Object v : list) {
                    if (v instanceof DBObject) {
                        markReferencedBinaries((DBObject) v, blobManager);
                    } else {
                        markReferencedBinary(v, blobManager);
                    }
                }
            } else if (value instanceof Object[]) {
                for (Object v : (Object[]) value) {
                    markReferencedBinary(v, blobManager);
                }
            } else if (value instanceof DBObject) {
                markReferencedBinaries((DBObject) value, blobManager);
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

    protected static final DBObject LOCK_FIELDS;

    static {
        LOCK_FIELDS = new BasicDBObject();
        LOCK_FIELDS.put(KEY_LOCK_OWNER, ONE);
        LOCK_FIELDS.put(KEY_LOCK_CREATED, ONE);
    }

    protected static final DBObject UNSET_LOCK_UPDATE = new BasicDBObject(MONGODB_UNSET, LOCK_FIELDS);

    @Override
    public Lock getLock(String id) {
        if (log.isTraceEnabled()) {
            logQuery(id, LOCK_FIELDS);
        }
        DBObject res = coll.findOne(new BasicDBObject(idKey, id), LOCK_FIELDS);
        if (res == null) {
            // document not found
            throw new DocumentNotFoundException(id);
        }
        String owner = (String) res.get(KEY_LOCK_OWNER);
        if (owner == null) {
            // not locked
            return null;
        }
        Calendar created = (Calendar) converter.scalarToSerializable(res.get(KEY_LOCK_CREATED));
        return new Lock(owner, created);
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        DBObject query = new BasicDBObject(idKey, id);
        query.put(KEY_LOCK_OWNER, null); // select doc if no lock is set
        DBObject setLock = new BasicDBObject();
        setLock.put(KEY_LOCK_OWNER, lock.getOwner());
        setLock.put(KEY_LOCK_CREATED, converter.serializableToBson(lock.getCreated()));
        DBObject setLockUpdate = new BasicDBObject(MONGODB_SET, setLock);
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: FINDANDMODIFY " + query + " UPDATE " + setLockUpdate);
        }
        DBObject res = coll.findAndModify(query, null, null, false, setLockUpdate, false, false);
        if (res != null) {
            // found a doc to lock
            return null;
        } else {
            // doc not found, or lock owner already set
            // get the old lock
            if (log.isTraceEnabled()) {
                logQuery(id, LOCK_FIELDS);
            }
            DBObject old = coll.findOne(new BasicDBObject(idKey, id), LOCK_FIELDS);
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
        DBObject query = new BasicDBObject(idKey, id);
        if (owner != null) {
            // remove if owner matches or null
            // implements LockManager.canLockBeRemoved inside MongoDB
            Object ownerOrNull = Arrays.asList(owner, null);
            query.put(KEY_LOCK_OWNER, new BasicDBObject(QueryOperators.IN, ownerOrNull));
        } // else unconditional remove
        // remove the lock
        DBObject old = coll.findAndModify(query, null, null, false, UNSET_LOCK_UPDATE, false, false);
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
            old = coll.findOne(new BasicDBObject(idKey, id), LOCK_FIELDS);
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
