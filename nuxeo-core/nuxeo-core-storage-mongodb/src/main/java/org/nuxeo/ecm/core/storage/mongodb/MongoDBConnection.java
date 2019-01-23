/*
 * (C) Copyright 2014-2019 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.COUNTER_FIELD;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.COUNTER_NAME_UUID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.FULLTEXT_INDEX_NAME;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.LANGUAGE_FIELD;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.LONG_ZERO;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_ID;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.MONGODB_SET;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.ONE;
import static org.nuxeo.ecm.core.storage.mongodb.MongoDBRepository.ZERO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSConnection;
import org.nuxeo.ecm.core.storage.dbs.DBSConnectionBase;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase.IdType;
import org.nuxeo.ecm.core.storage.dbs.DBSStateFlattener;
import org.nuxeo.ecm.core.storage.dbs.DBSTransactionState.ChangeTokenUpdater;

import com.mongodb.QueryOperators;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * MongoDB implementation of a {@link DBSConnection}.
 *
 * @since 5.9.4
 */
public class MongoDBConnection extends DBSConnectionBase {

    private static final Log log = LogFactory.getLog(MongoDBConnection.class);

    protected final MongoCollection<Document> collection;

    /** The key to use to store the id in the database. */
    protected final String idKey;

    /** True if we don't use MongoDB's native "_id" key to store the id. */
    protected final boolean useCustomId;

    protected final MongoDBConverter converter;

    protected ClientSession clientSession;

    protected boolean transactionStarted;

    public MongoDBConnection(MongoDBRepository repository) {
        super(repository);
        collection = repository.getCollection();
        idKey = repository.getIdKey();
        useCustomId = KEY_ID.equals(idKey);
        converter = repository.getConverter();
        if (repository.supportsSessions()) {
            clientSession = repository.getClient().startSession();
        } else {
            clientSession = null;
        }
        initRepository();
    }

    @Override
    public void close() {
        if (clientSession != null) {
            clientSession.close();
            clientSession = null;
        }
    }

    @Override
    public void begin() {
        if (clientSession != null) {
            clientSession.startTransaction();
            transactionStarted = true;
        }
    }

    @Override
    public void commit() {
        if (clientSession != null) {
            try {
                clientSession.commitTransaction();
            } finally {
                transactionStarted = false;
            }
        }
    }

    @Override
    public void rollback() {
        if (clientSession != null) {
            try {
                clientSession.abortTransaction();
            } finally {
                transactionStarted = false;
            }
        }
    }

    /** Initializes the repository. This is not transactional. */
    protected void initRepository() {
        // create required indexes
        // code does explicit queries on those
        if (useCustomId) {
            collection.createIndex(Indexes.ascending(idKey));
        }
        collection.createIndex(Indexes.ascending(KEY_PARENT_ID));
        collection.createIndex(Indexes.ascending(KEY_ANCESTOR_IDS));
        collection.createIndex(Indexes.ascending(KEY_VERSION_SERIES_ID));
        collection.createIndex(Indexes.ascending(KEY_PROXY_TARGET_ID));
        collection.createIndex(Indexes.ascending(KEY_PROXY_VERSION_SERIES_ID));
        collection.createIndex(Indexes.ascending(KEY_READ_ACL));
        collection.createIndex(Indexes.ascending(KEY_PARENT_ID, KEY_NAME));
        // often used in user-generated queries
        collection.createIndex(Indexes.ascending(KEY_PRIMARY_TYPE));
        collection.createIndex(Indexes.ascending(KEY_LIFECYCLE_STATE));
        collection.createIndex(Indexes.ascending(KEY_IS_TRASHED));
        if (!repository.isFulltextDisabled()) {
            collection.createIndex(Indexes.ascending(KEY_FULLTEXT_JOBID));
        }
        collection.createIndex(Indexes.ascending(KEY_ACP + "." + KEY_ACL + "." + KEY_ACE_USER));
        collection.createIndex(Indexes.ascending(KEY_ACP + "." + KEY_ACL + "." + KEY_ACE_STATUS));
        // TODO configure these from somewhere else
        collection.createIndex(Indexes.descending("dc:modified"));
        collection.createIndex(Indexes.ascending("rend:renditionName"));
        collection.createIndex(Indexes.ascending("drv:subscriptions.enabled"));
        collection.createIndex(Indexes.ascending("collectionMember:collectionIds"));
        collection.createIndex(Indexes.ascending("nxtag:tags"));
        if (!repository.isFulltextSearchDisabled()) {
            Bson indexKeys = Indexes.compoundIndex( //
                    Indexes.text(KEY_FULLTEXT_SIMPLE), //
                    Indexes.text(KEY_FULLTEXT_BINARY) //
            );
            IndexOptions indexOptions = new IndexOptions().name(FULLTEXT_INDEX_NAME).languageOverride(LANGUAGE_FIELD);
            collection.createIndex(indexKeys, indexOptions);
        }
        // check root presence
        if (collection.countDocuments(Filters.eq(idKey, getRootId())) > 0) {
            return;
        }
        // create basic repository structure needed
        if (repository.getIdType() == IdType.sequence || DBSRepositoryBase.DEBUG_UUIDS) {
            // create the id counter
            MongoCollection<Document> countersColl = ((MongoDBRepository) repository).getCountersCollection();
            Document idCounter = new Document();
            idCounter.put(MONGODB_ID, COUNTER_NAME_UUID);
            idCounter.put(COUNTER_FIELD, LONG_ZERO);
            countersColl.insertOne(idCounter);
        }
        initRoot();
    }

    @Override
    public String generateNewId() {
        if (repository.getIdType() == IdType.sequence || DBSRepositoryBase.DEBUG_UUIDS) {
            Long id = ((MongoDBRepository) repository).getNextSequenceId();
            if (DBSRepositoryBase.DEBUG_UUIDS) {
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
        insertOne(doc);
        // TODO dupe exception
        // throw new DocumentException("Already exists: " + id);
    }

    @Override
    public void createStates(List<State> states) {
        List<Document> docs = states.stream().map(converter::stateToBson).collect(Collectors.toList());
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: CREATE ["
                    + docs.stream().map(doc -> doc.get(idKey).toString()).collect(Collectors.joining(", ")) + "]: "
                    + docs);
        }
        insertMany(docs);
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
            UpdateResult w = updateMany(filter, update);
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
        DeleteResult w = deleteMany(filter);
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
        return find(filter).projection(projection).first() != null;
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
        MongoCursor<Document> cursor = find(filter).limit(limit) //
                                                   .projection(projection)
                                                   .iterator();
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
        MongoDBRepositoryQueryBuilder builder = new MongoDBRepositoryQueryBuilder((MongoDBRepository) repository,
                evaluator.getExpression(), evaluator.getSelectClause(), orderByClause, evaluator.pathResolver,
                evaluator.fulltextSearchDisabled);
        builder.walk();
        if (builder.hasFulltext && repository.isFulltextSearchDisabled()) {
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
        try (MongoCursor<Document> cursor = find(filter).projection(keys) //
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
                totalSize = countDocuments(filter);
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
                totalSize = countDocuments(filter, new CountOptions().limit(countUpTo + 1));
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
        MongoDBCursorService cursorService = ((MongoDBRepository) repository).getCursorService();
        cursorService.checkForTimedOutScroll();
        MongoDBRepositoryQueryBuilder builder = new MongoDBRepositoryQueryBuilder((MongoDBRepository) repository,
                evaluator.getExpression(), evaluator.getSelectClause(), null, evaluator.pathResolver,
                evaluator.fulltextSearchDisabled);
        builder.walk();
        if (builder.hasFulltext && repository.isFulltextSearchDisabled()) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        Bson filter = builder.getQuery();
        addPrincipals((Document) filter, evaluator.principals);
        Bson keys = builder.getProjection();
        if (log.isTraceEnabled()) {
            logQuery(filter, keys, null, 0, 0);
        }

        MongoCursor<Document> cursor = find(filter).projection(keys) //
                                                   .batchSize(batchSize)
                                                   .iterator();
        String scrollId = cursorService.registerCursor(cursor, batchSize, keepAliveSeconds);
        return scroll(scrollId);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        MongoDBCursorService cursorService = ((MongoDBRepository) repository).getCursorService();
        return cursorService.scroll(scrollId);
    }

    protected void addPrincipals(Document query, Set<String> principals) {
        if (principals != null) {
            Document inPrincipals = new Document(QueryOperators.IN, new ArrayList<>(principals));
            query.put(DBSDocument.KEY_READ_ACL, inPrincipals);
        }
    }

    protected static final Bson LOCK_FIELDS = Projections.include(KEY_LOCK_OWNER, KEY_LOCK_CREATED);

    protected static final Bson UNSET_LOCK_UPDATE = Updates.combine(Updates.unset(KEY_LOCK_OWNER),
            Updates.unset(KEY_LOCK_CREATED));

    @Override
    public Lock getLock(String id) {
        if (log.isTraceEnabled()) {
            logQuery(id, LOCK_FIELDS);
        }
        // we do NOT want to use clientSession here because locks must be non-transactional
        Document res = collection.find(Filters.eq(idKey, id)).projection(LOCK_FIELDS).first();
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
        // we do NOT want to use clientSession here because locks must be non-transactional
        Document res = collection.findOneAndUpdate(filter, setLock);
        if (res != null) {
            // found a doc to lock
            return null;
        } else {
            // doc not found, or lock owner already set
            // get the old lock
            if (log.isTraceEnabled()) {
                logQuery(id, LOCK_FIELDS);
            }
            // we do NOT want to use clientSession here because locks must be non-transactional
            Document old = collection.find(Filters.eq(idKey, id)).projection(LOCK_FIELDS).first();
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
        // we do NOT want to use clientSession here because locks must be non-transactional
        Document old = collection.findOneAndUpdate(filter, UNSET_LOCK_UPDATE);
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
            // we do NOT want to use clientSession here because locks must be non-transactional
            old = collection.find(Filters.eq(idKey, id)).projection(LOCK_FIELDS).first();
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

    protected void insertOne(Document document) {
        if (transactionStarted) {
            collection.insertOne(clientSession, document);
        } else {
            collection.insertOne(document);
        }
    }

    protected void insertMany(List<Document> documents) {
        if (transactionStarted) {
            collection.insertMany(clientSession, documents);
        } else {
            collection.insertMany(documents);
        }
    }

    protected UpdateResult updateMany(Bson filter, Bson update) {
        if (transactionStarted) {
            return collection.updateMany(clientSession, filter, update);
        } else {
            return collection.updateMany(filter, update);
        }
    }

    protected DeleteResult deleteMany(Bson filter) {
        if (transactionStarted) {
            return collection.deleteMany(clientSession, filter);
        } else {
            return collection.deleteMany(filter);
        }
    }

    protected FindIterable<Document> find(Bson filter) {
        if (transactionStarted) {
            return collection.find(clientSession, filter);
        } else {
            return collection.find(filter);
        }
    }

    protected long countDocuments(Bson filter) {
        if (transactionStarted) {
            return collection.countDocuments(clientSession, filter);
        } else {
            return collection.countDocuments(filter);
        }
    }

    protected long countDocuments(Bson filter, CountOptions options) {
        if (transactionStarted) {
            return collection.countDocuments(clientSession, filter, options);
        } else {
            return collection.countDocuments(filter, options);
        }
    }

}
