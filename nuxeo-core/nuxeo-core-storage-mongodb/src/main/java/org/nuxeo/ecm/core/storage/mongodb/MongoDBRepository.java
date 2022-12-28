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

import static com.mongodb.ErrorCategory.DUPLICATE_KEY;
import static com.mongodb.ErrorCategory.fromErrorCode;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_STATUS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACE_USER;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ACP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ANCESTOR_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_BLOB_KEYS;
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
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_RETAIN_UNTIL;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_VERSION_SERIES_ID;

import java.io.Serializable;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.resource.spi.ConnectionManager;

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
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.ecm.core.storage.dbs.DBSStateFlattener;
import org.nuxeo.ecm.core.storage.dbs.DBSTransactionState.ConditionalUpdates;
import org.nuxeo.ecm.core.storage.mongodb.MongoDBConverter.ConditionsAndUpdates;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBComponent.MongoDBCountHelper;
import org.nuxeo.runtime.mongodb.MongoDBConnectionService;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.MongoQueryException;
import com.mongodb.MongoSocketReadTimeoutException;
import com.mongodb.MongoWriteException;
import com.mongodb.QueryOperators;
import com.mongodb.bulk.BulkWriteError;
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
import com.mongodb.client.model.UpdateOptions;
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

    protected static final Random RANDOM = new SecureRandom();

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

    /** @since 11.5 */
    public static final String MONGODB_PULLALL = "$pullAll";

    public static final String MONGODB_EACH = "$each";

    public static final String MONGODB_META = "$meta";

    public static final String MONGODB_TYPE = "$type";

    public static final String MONGODB_EXISTS = "$exists";

    public static final String MONGODB_TEXT_SCORE = "textScore";

    private static final String FULLTEXT_INDEX_NAME = "fulltext";

    private static final String LANGUAGE_FIELD = "__language";

    protected static final String COUNTER_NAME_UUID = "ecm:id";

    protected static final String COUNTER_FIELD = "seq";

    protected static final int SEQUENCE_RANDOMIZED_BLOCKSIZE_DEFAULT = 1000;

    /**
     * @since 11.1
     */
    protected final String databaseID;

    protected final MongoCollection<Document> coll;

    protected final MongoCollection<Document> countersColl;

    protected final MongoCollection<Document> settingsColl;

    /**
     * Default maximum execution time for a query.
     *
     * @since 11.1
     */
    protected static final Duration MAX_TIME_DEFAULT = Duration.ofHours(1);

    protected static final String SETTING_VALUE = "value";

    /**
     * Settings key to determine whether {@code ecm:blobKeys} is supported.
     * <p>
     * The value is {@code true} on new or migrated repositories.
     */
    protected static final String SETTING_DENORMALIZED_BLOB_KEYS = "denormalizedBlobKeys";

    /** @since 2021.14 */
    protected static final String GC_NO_CURSOR_TIMEOUT = "nuxeo.mongodb.gc.noCursorTimeout";

    /** The key to use to store the id in the database. */
    protected String idKey;

    /** True if we don't use MongoDB's native "_id" key to store the id. */
    protected boolean useCustomId;

    /** Number of values still available in the in-memory sequence. */
    protected long sequenceLeft;

    /**
     * Last value or randomized value used from the in-memory sequence.
     * <p>
     * When used as a randomized sequence, this value (and the rest of the next block) may only be used after a
     * successful update of the in-database version for the next task needing a randomized value.
     */
    protected long sequenceLastValue;

    /** Sequence allocation block size. */
    protected long sequenceBlockSize;

    protected final MongoDBConverter converter;

    protected final CursorService<MongoCursor<Document>, Document, String> cursorService;

    /**
     * Maximum execution time for a query when outside of a transaction.
     *
     * @since 11.1
     */
    protected final long maxTimeMS;

    protected boolean supportsDenormalizedBlobKeys;

    public MongoDBRepository(ConnectionManager cm, MongoDBRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor);
        MongoDBConnectionService mongoService = Framework.getService(MongoDBConnectionService.class);
        // prefix with repository/ to group repository connection
        databaseID = REPOSITORY_CONNECTION_PREFIX + descriptor.name;
        MongoDatabase database = mongoService.getDatabase(databaseID);
        coll = database.getCollection(descriptor.name);
        countersColl = database.getCollection(descriptor.name + ".counters");
        settingsColl = database.getCollection(descriptor.name + ".settings");
        Document buildInfo = database.runCommand(new Document("buildInfo", ONE));
        String serverVersion = buildInfo.getString("version");
        Duration maxTime = mongoService.getConfig(databaseID).maxTime;
        if (maxTime == null) {
            maxTime = MAX_TIME_DEFAULT;
        }
        maxTimeMS = maxTime.toMillis();
        if (Boolean.TRUE.equals(descriptor.nativeId)) {
            idKey = MONGODB_ID;
        } else {
            idKey = KEY_ID;
        }
        useCustomId = KEY_ID.equals(idKey);
        if (idType == IdType.sequence || idType == IdType.sequenceHexRandomized || DEBUG_UUIDS) {
            Integer sbs = descriptor.sequenceBlockSize;
            if (sbs == null) {
                sequenceBlockSize = idType == IdType.sequenceHexRandomized ? SEQUENCE_RANDOMIZED_BLOCKSIZE_DEFAULT : 1;
            } else {
                sequenceBlockSize = sbs.longValue();
            }
            sequenceLeft = 0;
        }
        Set<String> idValuesKeys;
        if (idType == IdType.sequenceHexRandomized) {
            // store these ids as longs
            idValuesKeys = DBSSession.ID_VALUES_KEYS;
        } else {
            idValuesKeys = Collections.emptySet();
        }
        converter = new MongoDBConverter(useCustomId ? null : KEY_ID, DBSSession.TRUE_OR_NULL_BOOLEAN_KEYS, idValuesKeys);
        converter.setServerVersion(serverVersion);
        cursorService = new CursorService<>(ob -> (String) converter.getFromBson(ob, idKey, KEY_ID));
        initRepository(descriptor);
    }

    @Override
    public List<IdType> getAllowedIdTypes() {
        return Arrays.asList(IdType.varchar, IdType.sequence, IdType.sequenceHexRandomized);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        cursorService.clear();
    }

    /**
     * @deprecated since 11.1. use {@link #initRepository(MongoDBRepositoryDescriptor)} instead.
     */
    @Deprecated
    protected void initRepository() {
        initRepository(null);
    }

    /**
     * Initializes the MongoDB repository
     *
     * @param descriptor the MongoDB repository descriptor
     * @since 11.1
     */
    protected void initRepository(MongoDBRepositoryDescriptor descriptor) {
        // check root presence
        if (countDocuments(converter.filterEq(KEY_ID, getRootId())) > 0) {
            readSettings();
            return;
        }
        // create required indexes
        // code does explicit queries on those
        if (useCustomId) {
            coll.createIndex(Indexes.ascending(KEY_ID), new IndexOptions().unique(true));
        }
        coll.createIndex(Indexes.ascending(KEY_PARENT_ID));
        coll.createIndex(Indexes.ascending(KEY_ANCESTOR_IDS));
        coll.createIndex(Indexes.ascending(KEY_VERSION_SERIES_ID));
        coll.createIndex(Indexes.ascending(KEY_PROXY_TARGET_ID));
        coll.createIndex(Indexes.ascending(KEY_PROXY_VERSION_SERIES_ID));
        coll.createIndex(Indexes.ascending(KEY_READ_ACL));
        IndexOptions parentNameIndexOptions = new IndexOptions();
        if (descriptor != null) {
            parentNameIndexOptions.unique(Boolean.TRUE.equals(descriptor.getChildNameUniqueConstraintEnabled()))
                                  .partialFilterExpression(Filters.exists(KEY_PARENT_ID));
        }
        coll.createIndex(Indexes.ascending(KEY_PARENT_ID, KEY_NAME), parentNameIndexOptions);
        // often used in user-generated queries
        coll.createIndex(Indexes.ascending(KEY_PRIMARY_TYPE));
        coll.createIndex(Indexes.ascending(KEY_LIFECYCLE_STATE));
        coll.createIndex(Indexes.ascending(KEY_IS_TRASHED));
        coll.createIndex(Indexes.ascending(KEY_RETAIN_UNTIL));
        coll.createIndex(Indexes.ascending(KEY_BLOB_KEYS));
        if (!isFulltextDisabled()) {
            coll.createIndex(Indexes.ascending(KEY_FULLTEXT_JOBID));
        }
        coll.createIndex(Indexes.ascending(KEY_ACP + "." + KEY_ACL + "." + KEY_ACE_USER));
        coll.createIndex(Indexes.ascending(KEY_ACP + "." + KEY_ACL + "." + KEY_ACE_STATUS));
        // TODO configure these from somewhere else
        coll.createIndex(Indexes.descending("dc:modified"));
        coll.createIndex(Indexes.ascending("rend:renditionName"));
        coll.createIndex(Indexes.ascending("rend:sourceId"));
        coll.createIndex(Indexes.ascending("rend:sourceVersionableId"));
        coll.createIndex(Indexes.ascending("drv:subscriptions.enabled"));
        coll.createIndex(Indexes.ascending("collection:documentIds"));
        coll.createIndex(Indexes.ascending("collectionMember:collectionIds"));
        coll.createIndex(Indexes.ascending("nxtag:tags.label"));
        // TODO remove it when PropertyCommentManager will be removed
        coll.createIndex(Indexes.ascending("comment:parentId"));
        coll.createIndex(Indexes.ascending("annotation:xpath"));
        if (!isFulltextSearchDisabled()) {
            Bson indexKeys = Indexes.compoundIndex( //
                    Indexes.text(KEY_FULLTEXT_SIMPLE), //
                    Indexes.text(KEY_FULLTEXT_BINARY) //
            );
            IndexOptions indexOptions = new IndexOptions().name(FULLTEXT_INDEX_NAME).languageOverride(LANGUAGE_FIELD);
            coll.createIndex(indexKeys, indexOptions);
        }
        // create basic repository structure needed
        if (idType == IdType.sequence || idType == IdType.sequenceHexRandomized || DEBUG_UUIDS) {
            // create the id counter
            long counter;
            if (idType == IdType.sequenceHexRandomized) {
                counter = randomInitialSeed();
            } else {
                counter = 0;
            }
            Document idCounter = new Document();
            idCounter.put(MONGODB_ID, COUNTER_NAME_UUID);
            idCounter.put(COUNTER_FIELD, Long.valueOf(counter));
            countersColl.insertOne(idCounter);
        }
        initSettings();
        initRoot();
    }

    protected void initSettings() {
        supportsDenormalizedBlobKeys = true;
        Bson filter = Filters.eq(MONGODB_ID, SETTING_DENORMALIZED_BLOB_KEYS);
        Bson update = Updates.set(SETTING_VALUE, supportsDenormalizedBlobKeys);
        settingsColl.updateOne(filter, update, new UpdateOptions().upsert(true));
        initCapabilities();
    }

    protected void readSettings() {
        Document doc = settingsColl.find(Filters.eq(MONGODB_ID, SETTING_DENORMALIZED_BLOB_KEYS)).first();
        if (doc == null) {
            supportsDenormalizedBlobKeys = false;
        } else {
            supportsDenormalizedBlobKeys = doc.getBoolean(SETTING_VALUE, false);
        }
        initCapabilities();
    }

    protected void initCapabilities() {
        capabilities.put(CAPABILITY_QUERY_BLOB_KEYS, supportsDenormalizedBlobKeys);
    }

    protected synchronized long getNextSequenceId() {
        if (idType == IdType.sequence) {
            if (sequenceLeft == 0) {
                sequenceLeft = sequenceBlockSize;
                sequenceLastValue = updateSequence();
            }
            sequenceLastValue++;
        } else { // idType == IdType.sequenceHexRandomized
            if (sequenceLeft == 0) {
                sequenceLeft = sequenceBlockSize;
                sequenceLastValue = updateRandomizedSequence();
            }
            sequenceLastValue = xorshift(sequenceLastValue);
        }
        sequenceLeft--;
        return sequenceLastValue;
    }

    /**
     * Allocates a new sequence block. The database contains the last value from the last block.
     */
    protected long updateSequence() {
        Bson filter = Filters.eq(MONGODB_ID, COUNTER_NAME_UUID);
        Bson update = Updates.inc(COUNTER_FIELD, Long.valueOf(sequenceBlockSize));
        Document idCounter = countersColl.findOneAndUpdate(filter, update,
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
        if (idCounter == null) {
            throw new NuxeoException("Repository id counter not initialized");
        }
        return idCounter.getLong(COUNTER_FIELD).longValue() - sequenceBlockSize;
    }

    /**
     * Updates the randomized sequence, using xorshift.
     */
    protected Long tryUpdateRandomizedSequence() {
        // find the current value
        Bson filter = Filters.eq(MONGODB_ID, COUNTER_NAME_UUID);
        Document res = countersColl.find(filter).first();
        if (res == null) {
            throw new NuxeoException(
                    "Failed to read " + filter + " in collection " + countersColl.getNamespace());
        }
        Long lastValue = res.getLong(COUNTER_FIELD);
        // find the next value after this block is done
        long newValue = xorshift(lastValue, sequenceBlockSize);
        // store the next value for whoever needs it next
        Bson updateFilter = Filters.and( //
                filter, //
                Filters.eq(COUNTER_FIELD, lastValue)
        );
        Bson update = Updates.set(COUNTER_FIELD, newValue);
        log.trace("MongoDB: FINDANDMODIFY " + updateFilter + " UPDATE " + update);
        boolean updated = countersColl.findOneAndUpdate(updateFilter, update) != null;
        if (updated) {
            return lastValue;
        } else {
            log.trace("MongoDB:    -> FAILED (will retry)");
            return null;
        }
    }

    protected static final int NB_TRY = 15;

    protected long updateRandomizedSequence() {
        long sleepDuration = 1; // start with 1ms
        for (int i = 0; i < NB_TRY; i++) {
            Long value = tryUpdateRandomizedSequence();
            if (value != null) {
                return value.longValue();
            }
            try {
                Thread.sleep(sleepDuration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NuxeoException();
            }
            sleepDuration *= 2; // exponential backoff
            sleepDuration += System.nanoTime() % 4; // random jitter
        }
        throw new ConcurrentUpdateException("Failed to update randomized sequence");
    }

    /** Initial seed generation. */
    protected long randomInitialSeed() {
        long seed;
        do {
            seed = RANDOM.nextLong();
        } while (seed == 0);
        return seed;
    }

    /** Iterated version of xorshift. */
    protected long xorshift(long n, long times) {
        for (long i = 0; i < times; i++) {
            n = xorshift(n);
        }
        return n;
    }

    /**
     * xorshift algorithm from George Marsaglia, with period 2^64 - 1.
     *
     * @see https://www.jstatsoft.org/article/view/v008i14/xorshift.pdf
     */
    protected long xorshift(long n) {
        n ^= (n << 13);
        n ^= (n >>> 7);
        n ^= (n << 17);
        return n;
    }

    @Override
    public String generateNewId() {
        if (idType == IdType.sequence || idType == IdType.sequenceHexRandomized || DEBUG_UUIDS) {
            long id = getNextSequenceId();
            if (DEBUG_UUIDS) {
                return "UUID_" + id;
            } else if (idType == IdType.sequence) {
                return String.valueOf(id);
            } else { // idType == IdType.sequenceHexRandomized
                // hex version filled to 16 chars
                String hex = Long.toHexString(id);
                int nz = 16 - hex.length();
                if (nz > 0) {
                    hex = "0000000000000000".substring(16 - nz) + hex;
                }
                return hex;
            }
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
        try {
            coll.insertOne(doc);
        } catch (DuplicateKeyException dke) {
            log.trace("MongoDB:    -> DUPLICATE KEY: " + doc.get(idKey));
            throw new ConcurrentUpdateException(dke);
        }
    }

    @Override
    public void createStates(List<State> states) {
        List<Document> docs = states.stream().map(converter::stateToBson).collect(Collectors.toList());
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: CREATE ["
                    + docs.stream().map(doc -> doc.get(idKey).toString()).collect(Collectors.joining(", ")) + "]: "
                    + docs);
        }
        try {
            coll.insertMany(docs);
        } catch (MongoBulkWriteException mbwe) {
            List<String> duplicates = mbwe.getWriteErrors()
                                          .stream()
                                          .filter(wr -> DUPLICATE_KEY.equals(fromErrorCode(wr.getCode())))
                                          .map(BulkWriteError::getMessage)
                                          .collect(Collectors.toList());
            // Avoid hiding any others bulk errors
            if (duplicates.size() == mbwe.getWriteErrors().size()) {
                log.trace("MongoDB:    -> DUPLICATE KEY: " + duplicates);
                ConcurrentUpdateException concurrentUpdateException = new ConcurrentUpdateException("Concurrent update");
                duplicates.forEach(concurrentUpdateException::addInfo);
                throw concurrentUpdateException;
            }

            throw mbwe;
        }
    }

    @Override
    public State readState(String id) {
        return findOne(converter.filterEq(KEY_ID, id));
    }

    @Override
    public State readPartialState(String id, Collection<String> keys) {
        Document fields = new Document();
        keys.forEach(key -> fields.put(converter.keyToBson(key), ONE));
        return findOne(converter.filterEq(KEY_ID, id), fields);
    }

    @Override
    public List<State> readStates(List<String> ids) {
        return findAll(converter.filterIn(KEY_ID, ids));
    }

    @Override
    public void updateState(String id, StateDiff diff, ConditionalUpdates conditionalUpdates) {
        ConditionsAndUpdates conditionsAndUpdates = converter.diffToBson(diff);
        for (Document update : conditionsAndUpdates.updates) {
            Document filter = new Document(conditionsAndUpdates.conditions);
            converter.putToBson(filter, KEY_ID, id);
            if (conditionalUpdates != null) {
                // assume bson is identical to dbs internals
                // condition works even if value is null
                filter.putAll(conditionalUpdates.getConditions());
                Document set = (Document) update.computeIfAbsent(MONGODB_SET, k -> new Document());
                set.putAll(conditionalUpdates.getUpdates());
                conditionalUpdates.finish();
            }
            if (filter.size() == 1) {
                if (log.isTraceEnabled()) {
                    log.trace("MongoDB: UPDATE " + id + ": " + update);
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("MongoDB: UPDATE " + id + ": IF " + filter + " THEN " + update);
                }
            }
            try {
                UpdateResult w = coll.updateMany(filter, update);
                if (w.getModifiedCount() != 1) {
                    log.trace("MongoDB:    -> CONCURRENT UPDATE: " + id);
                    throw new ConcurrentUpdateException(id);
                }
            } catch (MongoWriteException mwe) {
                if (DUPLICATE_KEY.equals(fromErrorCode(mwe.getCode()))) {
                    log.trace("MongoDB:    -> DUPLICATE KEY: " + id);
                    throw new ConcurrentUpdateException(mwe.getError().getMessage(), mwe);
                }
                throw mwe;
            }
        }
    }

    @Override
    public void deleteStates(Set<String> ids) {
        Bson filter = converter.filterIn(KEY_ID, ids);
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

    protected NuxeoException newQueryException(String message, MongoException cause, Bson filter) {
        NuxeoException exc = new NuxeoException(message, cause);
        if (filter != null) {
            String msg;
            if (filter instanceof Document) {
                msg = ((Document) filter).toJson();
            } else {
                msg = filter.toString();
            }
            exc.addInfo("Filter: " + msg);
        }
        return exc;
    }

    protected NuxeoException newQueryTimeout(MongoException cause, Bson filter) {
        return newQueryException("Query timed out as a result of the maximum operation time being exceeded", cause,
                filter);
    }

    protected NuxeoException newQueryTimeoutClient(MongoException cause, Bson filter) {
        return newQueryException("Query timed out on client side after socketTimeout exceeded", cause, filter);
    }

    protected NuxeoException newQueryFailure(MongoException cause, Bson filter) {
        return newQueryException("Query operation failed on the server", cause, filter);
    }

    protected void logQuery(String id, Bson fields) {
        logQuery(converter.filterEq(KEY_ID, id), fields);
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
        converter.putToBson(filter, KEY_PARENT_ID, parentId);
        converter.putToBson(filter, KEY_NAME, name);
        addIgnoredIds(filter, ignored);
        return filter;
    }

    protected void addIgnoredIds(Document filter, Set<String> ignored) {
        if (!ignored.isEmpty()) {
            Document notInIds = new Document(QueryOperators.NIN, converter.listToBson(KEY_ID, ignored));
            filter.put(idKey, notInIds);
        }
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        Document filter = new Document();
        converter.putToBson(filter, key, value);
        addIgnoredIds(filter, ignored);
        return findAll(filter);
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        Document filter = new Document();
        converter.putToBson(filter, key1, value1);
        converter.putToBson(filter, key2, value2);
        addIgnoredIds(filter, ignored);
        return findAll(filter);
    }

    @Override
    public List<State> queryKeyValueWithOperator(String key1, Object value1, String key2, DBSQueryOperator operator,
            Object value2, Set<String> ignored) {
        Map<String, Object> comparatorAndValue;
        switch (operator) {
        case IN:
            comparatorAndValue = Collections.singletonMap(QueryOperators.IN, value2);
            break;
        case NOT_IN:
            comparatorAndValue = Collections.singletonMap(QueryOperators.NIN, value2);
            break;
        default:
            throw new IllegalArgumentException(String.format("Unknown operator: %s", operator));
        }
        Document filter = new Document();
        converter.putToBson(filter, key1, value1);
        converter.putToBson(filter, key2, comparatorAndValue);
        addIgnoredIds(filter, ignored);
        return findAll(filter);
    }

    @Override
    public Stream<State> getDescendants(String rootId, Set<String> keys) {
        return getDescendants(rootId, keys, 0);
    }

    @Override
    public Stream<State> getDescendants(String rootId, Set<String> keys, int limit) {
        Bson filter = converter.filterEq(KEY_ANCESTOR_IDS, rootId);
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
        Document filter = new Document();
        converter.putToBson(filter, key, value);
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
        try {
            return find(filter).projection(projection).first() != null;
        } catch (MongoExecutionTimeoutException e) {
            throw newQueryTimeout(e, filter);
        } catch (MongoSocketReadTimeoutException e) {
            throw newQueryTimeoutClient(e, filter);
        } catch (MongoQueryException e) {
            throw newQueryFailure(e, filter);
        }
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
        } catch (MongoExecutionTimeoutException e) {
            throw newQueryTimeout(e, filter);
        } catch (MongoSocketReadTimeoutException e) {
            throw newQueryTimeoutClient(e, filter);
        } catch (MongoQueryException e) {
            throw newQueryFailure(e, filter);
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
        MongoCursor<Document> cursor = find(filter).limit(limit).projection(projection).iterator();
        try {
            Set<Object> seen = new HashSet<>();
            Stream<State> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(cursor, 0), false) //
                                                .onClose(cursor::close)
                                                .filter(doc -> seen.add(doc.get(idKey)))
                                                // MongoDB cursors may return the same
                                                // object several times
                                                .map(converter::bsonToState);
            // the stream takes responsibility for closing the session
            completedAbruptly = false;
            return stream;
        } catch (MongoExecutionTimeoutException e) {
            throw newQueryTimeout(e, filter); // NOSONAR (cursor is not leaked)
        } catch (MongoSocketReadTimeoutException e) {
            throw newQueryTimeoutClient(e, filter); // NOSONAR (cursor is not leaked)
        } catch (MongoQueryException e) {
            throw newQueryFailure(e, filter); // NOSONAR (cursor is not leaked)
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
        MongoDBRepositoryQueryBuilder builder = new MongoDBRepositoryQueryBuilder(this, evaluator.getExpression(),
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
        try (MongoCursor<Document> cursor = find(filter)
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
        } catch (MongoExecutionTimeoutException e) {
            throw newQueryTimeout(e, filter);
        } catch (MongoSocketReadTimeoutException e) {
            throw newQueryTimeoutClient(e, filter);
        } catch (MongoQueryException e) {
            throw newQueryFailure(e, filter);
        }
        if (countUpTo == -1) {
            // count full size
            if (limit == 0) {
                totalSize = projections.size();
            } else if (manualProjection) {
                totalSize = -1; // unknown due to manual projection
            } else {
                totalSize = countDocuments(filter); // will return -2 if time out during count (e.g. too many results)
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
                // will return -2 if time out during count (e.g. too many results)
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
        MongoDBRepositoryQueryBuilder builder = new MongoDBRepositoryQueryBuilder(this, evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled);
        builder.walk();
        if (builder.hasFulltext && isFulltextSearchDisabled()) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        Bson filter = builder.getQuery();
        addPrincipals((Document) filter, evaluator.principals);
        Bson keys = builder.getProjection();
        if (log.isTraceEnabled()) {
            logQuery(filter, keys, null, 0, 0);
        }

        MongoCursor<Document> cursor;
        try {
            cursor = find(filter).projection(keys).batchSize(batchSize).iterator();
            cursor.hasNext(); // check timeout asap - NOSONAR
        } catch (MongoExecutionTimeoutException e) {
            throw newQueryTimeout(e, filter);
        } catch (MongoSocketReadTimeoutException e) {
            throw newQueryTimeoutClient(e, filter);
        } catch (MongoQueryException e) {
            throw newQueryFailure(e, filter);
        }
        String scrollId = cursorService.registerCursor(cursor, batchSize, keepAliveSeconds);
        return scroll(scrollId);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        try {
            return cursorService.scroll(scrollId);
        } catch (MongoExecutionTimeoutException e) {
            throw newQueryTimeout(e, null);
        } catch (MongoSocketReadTimeoutException e) {
            throw newQueryTimeoutClient(e, null);
        } catch (MongoQueryException e) {
            throw newQueryFailure(e, null);
        }
    }

    protected void addPrincipals(Document query, Set<String> principals) {
        if (principals != null) {
            Document inPrincipals = new Document(QueryOperators.IN, new ArrayList<>(principals));
            query.put(KEY_READ_ACL, inPrincipals);
        }
    }

    /**
     * Keys used for document projection when marking all binaries for GC.
     * <p>
     * Used when denormalized ecm:blobKeys is not available.
     */
    protected Bson binaryKeys;

    @Override
    protected void initBlobsPaths() {
        super.initBlobsPaths();
        // compute projections for when ecm:blobKeys is not available
        List<Bson>projections = new ArrayList<>(blobKeysPaths.size() + 1);
        projections.add(Projections.excludeId());
        blobKeysPaths.forEach(path -> projections.add(Projections.include(String.join(".", path))));
        binaryKeys = Projections.fields(projections);
    }

    @Override
    public void markReferencedBinaries() {
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        Bson filter;
        Bson projection;
        Consumer<Document> markReferencedBinaries;
        if (supportsDenormalizedBlobKeys) {
            filter = Filters.exists(KEY_BLOB_KEYS, true);
            projection = Projections.fields(Projections.excludeId(), Projections.include(KEY_BLOB_KEYS));
            markReferencedBinaries = doc -> markReferencedBinariesDenormalized(doc, blobManager);
        } else {
            filter = new Document();
            projection = binaryKeys;
            markReferencedBinaries = doc -> markReferencedBinaries(doc, blobManager);
        }
        boolean noCursorTimeout = Framework.getService(ConfigurationService.class).isBooleanTrue(GC_NO_CURSOR_TIMEOUT);
        if (log.isTraceEnabled()) {
            logQuery(filter, projection);
        }
        coll.find(filter).noCursorTimeout(noCursorTimeout).projection(projection).forEach(markReferencedBinaries);
    }

    protected void markReferencedBinariesDenormalized(Document ob, DocumentBlobManager blobManager) {
        Object blobKeys = ob.get(KEY_BLOB_KEYS);
        if (blobKeys instanceof List) {
            for (Object v : (List<?>) blobKeys) {
                if (v instanceof String) {
                    blobManager.markReferencedBinary((String) v, repositoryName);
                }
            }
        }
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
        Document res = coll.find(converter.filterEq(KEY_ID, id)).projection(LOCK_FIELDS).first();
        if (res == null) {
            // document not found
            throw new DocumentNotFoundException(id);
        }
        String owner = res.getString(KEY_LOCK_OWNER);
        if (owner == null) {
            // not locked
            return null;
        }
        Calendar created = (Calendar) converter.bsonToSerializable(KEY_LOCK_CREATED, res.get(KEY_LOCK_CREATED));
        return new Lock(owner, created);
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        Bson filter = Filters.and( //
                converter.filterEq(KEY_ID, id),
                Filters.exists(KEY_LOCK_OWNER, false) // select doc if no lock is set
        );
        Bson setLock = Updates.combine( //
                Updates.set(KEY_LOCK_OWNER, lock.getOwner()), //
                Updates.set(KEY_LOCK_CREATED, converter.serializableToBson(KEY_LOCK_CREATED, lock.getCreated())) //
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
            Document old = coll.find(converter.filterEq(KEY_ID, id)).projection(LOCK_FIELDS).first();
            if (old == null) {
                // document not found
                throw new DocumentNotFoundException(id);
            }
            String oldOwner = (String) old.get(KEY_LOCK_OWNER);
            Calendar oldCreated = (Calendar) converter.bsonToSerializable(KEY_LOCK_CREATED, old.get(KEY_LOCK_CREATED));
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
        Document filter = new Document();
        converter.putToBson(filter, KEY_ID, id);
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
                Calendar oldCreated = (Calendar) converter.bsonToSerializable(KEY_LOCK_CREATED, old.get(KEY_LOCK_CREATED));
                return new Lock(oldOwner, oldCreated);
            }
        } else {
            // doc not found, or lock owner didn't match
            // get the old lock
            if (log.isTraceEnabled()) {
                logQuery(id, LOCK_FIELDS);
            }
            old = coll.find(converter.filterEq(KEY_ID, id)).projection(LOCK_FIELDS).first();
            if (old == null) {
                // document not found
                throw new DocumentNotFoundException(id);
            }
            String oldOwner = (String) old.get(KEY_LOCK_OWNER);
            Calendar oldCreated = (Calendar) converter.bsonToSerializable(KEY_LOCK_CREATED, old.get(KEY_LOCK_CREATED));
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

    protected FindIterable<Document> find(Bson filter) {
        return coll.find(filter).maxTime(getMaxTimeMs(), MILLISECONDS);
    }

    protected long getMaxTimeMs() {
        long ttl = TransactionHelper.getTransactionTimeToLive();
        if (ttl < 0) {
            return maxTimeMS;
        }
        if (ttl >= 2) {
            // try to keep a margin to avoid transaction timeout
            return (ttl - 1) * 1000;
        }
        // returns at least 100ms (0 maxTime means no limit)
        return Math.max((ttl * 1000) - 100, 100);
    }

    protected long countDocuments(Bson filter) {
        return countDocuments(filter, new CountOptions());
    }

    protected long countDocuments(Bson filter, CountOptions options) {
        long maxTime = getMaxTimeMs();
        options.maxTime(maxTime, MILLISECONDS);
        try {
            return MongoDBCountHelper.countDocuments(databaseID, coll, filter, options);
        } catch (MongoExecutionTimeoutException | MongoSocketReadTimeoutException e) {
            log.warn(String.format("MongoDB timed out, maxTime=%dms, when computing total count with filters %s",
                    maxTime, filter.toString()));
            return -2;
        }
    }

}
