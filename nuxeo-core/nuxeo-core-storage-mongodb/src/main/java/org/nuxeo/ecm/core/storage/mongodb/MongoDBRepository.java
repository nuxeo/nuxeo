/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.core.api.ScrollResultImpl.emptyResult;
import static org.nuxeo.ecm.core.storage.State.NOP;
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
import java.lang.reflect.Array;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.resource.spi.ConnectionManager;

import com.mongodb.MongoClientOptions;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.ScrollResultImpl;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.ecm.core.storage.dbs.DBSStateFlattener;
import org.nuxeo.runtime.api.Framework;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.QueryOperators;
import com.mongodb.ServerAddress;
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

    protected static final int MONGODB_OPTION_CONNECTION_TIMEOUT_MS = 30000;

    protected static final int MONGODB_OPTION_SOCKET_TIMEOUT_MS = 60000;

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

    protected static Map<String, CursorResult> cursorResults = new ConcurrentHashMap<>();

    public MongoDBRepository(ConnectionManager cm, MongoDBRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor);
        try {
            mongoClient = newMongoClient(descriptor);
            coll = getCollection(descriptor, mongoClient);
            countersColl = getCountersCollection(descriptor, mongoClient);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
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
        initRepository();
    }

    @Override
    public List<IdType> getAllowedIdTypes() {
        return Arrays.asList(IdType.varchar, IdType.sequence);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        mongoClient.close();
    }

    // used also by unit tests
    public static MongoClient newMongoClient(MongoDBRepositoryDescriptor descriptor) throws UnknownHostException {
        MongoClient ret = null;
        String server = descriptor.server;
        if (StringUtils.isBlank(server)) {
            throw new NuxeoException("Missing <server> in MongoDB repository descriptor");
        }
        MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder()
                // Can help to prevent firewall disconnects inactive connection, option not available from URI
                .socketKeepAlive(true)
                // don't wait for ever by default, can be overridden using URI options
                .connectTimeout(MONGODB_OPTION_CONNECTION_TIMEOUT_MS)
                .socketTimeout(MONGODB_OPTION_SOCKET_TIMEOUT_MS)
                .description("Nuxeo");
        if (server.startsWith("mongodb://")) {
            // allow mongodb:// URI syntax for the server, to pass everything in one string
            ret = new MongoClient(new MongoClientURI(server, optionsBuilder));
        } else {
            ret = new MongoClient(new ServerAddress(server), optionsBuilder.build());
        }
        if (log.isDebugEnabled()) {
            log.debug("MongoClient initialized with options: " + ret.getMongoClientOptions().toString());
        }
        return ret;
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

    protected String keyToBson(String key) {
        if (useCustomId) {
            return key;
        } else {
            return KEY_ID.equals(key) ? idKey : key;
        }
    }

    protected Object valueToBson(Object value) {
        if (value instanceof State) {
            return stateToBson((State) value);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> values = (List<Object>) value;
            return listToBson(values);
        } else if (value instanceof Object[]) {
            return listToBson(Arrays.asList((Object[]) value));
        } else {
            return serializableToBson(value);
        }
    }

    protected DBObject stateToBson(State state) {
        DBObject ob = new BasicDBObject();
        for (Entry<String, Serializable> en : state.entrySet()) {
            Object val = valueToBson(en.getValue());
            if (val != null) {
                ob.put(keyToBson(en.getKey()), val);
            }
        }
        return ob;
    }

    protected List<Object> listToBson(List<Object> values) {
        ArrayList<Object> objects = new ArrayList<Object>(values.size());
        for (Object value : values) {
            objects.add(valueToBson(value));
        }
        return objects;
    }

    protected String bsonToKey(String key) {
        if (useCustomId) {
            return key;
        } else {
            return idKey.equals(key) ? KEY_ID : key;
        }
    }

    protected State bsonToState(DBObject ob) {
        if (ob == null) {
            return null;
        }
        State state = new State(ob.keySet().size());
        for (String key : ob.keySet()) {
            if (useCustomId && MONGODB_ID.equals(key)) {
                // skip native id
                continue;
            }
            state.put(bsonToKey(key), bsonToValue(ob.get(key)));
        }
        return state;
    }

    protected Serializable bsonToValue(Object value) {
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            if (list.isEmpty()) {
                return null;
            } else {
                Class<?> klass = Object.class;
                for (Object o : list) {
                    if (o != null) {
                        klass = scalarToSerializableClass(o.getClass());
                        break;
                    }
                }
                if (DBObject.class.isAssignableFrom(klass)) {
                    List<Serializable> l = new ArrayList<>(list.size());
                    for (Object el : list) {
                        l.add(bsonToState((DBObject) el));
                    }
                    return (Serializable) l;
                } else {
                    // turn the list into a properly-typed array
                    Object[] ar = (Object[]) Array.newInstance(klass, list.size());
                    int i = 0;
                    for (Object el : list) {
                        ar[i++] = scalarToSerializable(el);
                    }
                    return ar;
                }
            }
        } else if (value instanceof DBObject) {
            return bsonToState((DBObject) value);
        } else {
            return scalarToSerializable(value);
        }
    }

    public static class Updates {
        public BasicDBObject set = new BasicDBObject();

        public BasicDBObject unset = new BasicDBObject();

        public BasicDBObject push = new BasicDBObject();

        public BasicDBObject inc = new BasicDBObject();
    }

    /**
     * Constructs a list of MongoDB updates from the given {@link StateDiff}.
     * <p>
     * We need a list because some cases need two operations to avoid conflicts.
     */
    protected List<DBObject> diffToBson(StateDiff diff) {
        Updates updates = new Updates();
        diffToUpdates(diff, null, updates);
        UpdateListBuilder builder = new UpdateListBuilder();
        for (Entry<String, Object> en : updates.set.entrySet()) {
            builder.update(MONGODB_SET, en.getKey(), en.getValue());
        }
        for (Entry<String, Object> en : updates.unset.entrySet()) {
            builder.update(MONGODB_UNSET, en.getKey(), en.getValue());
        }
        for (Entry<String, Object> en : updates.push.entrySet()) {
            builder.update(MONGODB_PUSH, en.getKey(), en.getValue());
        }
        for (Entry<String, Object> en : updates.inc.entrySet()) {
            builder.update(MONGODB_INC, en.getKey(), en.getValue());
        }
        return builder.updateList;
    }

    /**
     * Update list builder to prevent several updates of the same field.
     * <p>
     * This happens if two operations act on two fields where one is a prefix of the other.
     * <p>
     * Example: Cannot update 'mylist.0.string' and 'mylist' at the same time (error 16837)
     *
     * @since 5.9.5
     */
    protected static class UpdateListBuilder {

        protected List<DBObject> updateList = new ArrayList<>(10);

        protected DBObject update;

        protected Set<String> prefixKeys;

        protected Set<String> keys;

        protected UpdateListBuilder() {
            newUpdate();
        }

        protected void update(String op, String key, Object value) {
            checkForConflict(key);
            DBObject map = (DBObject) update.get(op);
            if (map == null) {
                update.put(op, map = new BasicDBObject());
            }
            map.put(key, value);
        }

        /**
         * Checks if the key conflicts with one of the previous keys.
         * <p>
         * A conflict occurs if one key is equals to or is a prefix of the other.
         */
        protected void checkForConflict(String key) {
            List<String> pKeys = getPrefixKeys(key);
            if (conflictKeys(key, pKeys)) {
                newUpdate();
            }
            prefixKeys.addAll(pKeys);
            keys.add(key);
        }

        protected void newUpdate() {
            updateList.add(update = new BasicDBObject());
            prefixKeys = new HashSet<>();
            keys = new HashSet<>();
        }

        protected boolean conflictKeys(String key, List<String> subkeys) {
            if (prefixKeys.contains(key)) {
                return true;
            }
            for (String sk: subkeys) {
                if (keys.contains(sk)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * return a list of parents key
         * foo.0.bar -> [foo, foo.0, foo.0.bar]
         */
        protected List<String> getPrefixKeys(String key) {
            List<String> ret = new ArrayList<>(10);
            int i=0;
            while ((i = key.indexOf('.', i)) > 0) {
               ret.add(key.substring(0, i++));
            }
            ret.add(key);
            return ret;
        }

    }

    protected void diffToUpdates(StateDiff diff, String prefix, Updates updates) {
        String elemPrefix = prefix == null ? "" : prefix + '.';
        for (Entry<String, Serializable> en : diff.entrySet()) {
            String name = elemPrefix + en.getKey();
            Serializable value = en.getValue();
            if (value instanceof StateDiff) {
                diffToUpdates((StateDiff) value, name, updates);
            } else if (value instanceof ListDiff) {
                diffToUpdates((ListDiff) value, name, updates);
            } else if (value instanceof Delta) {
                diffToUpdates((Delta) value, name, updates);
            } else {
                // not a diff
                if (value == null) {
                    // for null values, beyond the space saving,
                    // it's important to unset the field instead of setting the value to null
                    // because $inc does not work on nulls but works on non-existent fields
                    updates.unset.put(name, ONE);
                } else {
                    updates.set.put(name, valueToBson(value));
                }
            }
        }
    }

    protected void diffToUpdates(ListDiff listDiff, String prefix, Updates updates) {
        if (listDiff.diff != null) {
            String elemPrefix = prefix == null ? "" : prefix + '.';
            int i = 0;
            for (Object value : listDiff.diff) {
                String name = elemPrefix + i;
                if (value instanceof StateDiff) {
                    diffToUpdates((StateDiff) value, name, updates);
                } else if (value != NOP) {
                    // set value
                    updates.set.put(name, valueToBson(value));
                }
                i++;
            }
        }
        if (listDiff.rpush != null) {
            Object pushed;
            if (listDiff.rpush.size() == 1) {
                // no need to use $each for one element
                pushed = valueToBson(listDiff.rpush.get(0));
            } else {
                pushed = new BasicDBObject(MONGODB_EACH, listToBson(listDiff.rpush));
            }
            updates.push.put(prefix, pushed);
        }
    }

    protected void diffToUpdates(Delta delta, String prefix, Updates updates) {
        // MongoDB can $inc a field that doesn't exist, it's treated as 0 BUT it doesn't work on null
        // so we ensure (in diffToUpdates) that we never store a null but remove the field instead
        Object inc = valueToBson(delta.getDeltaValue());
        updates.inc.put(prefix, inc);
    }

    protected Object serializableToBson(Object value) {
        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }
        return value;
    }

    protected Serializable scalarToSerializable(Object val) {
        if (val instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) val);
            return cal;
        }
        return (Serializable) val;
    }

    protected Class<?> scalarToSerializableClass(Class<?> klass) {
        if (Date.class.isAssignableFrom(klass)) {
            return Calendar.class;
        }
        return klass;
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
        if(!isFulltextDisabled()) {
            coll.createIndex(new BasicDBObject(KEY_FULLTEXT_JOBID, ONE));
        }
        coll.createIndex(new BasicDBObject(KEY_ACP + "." + KEY_ACL + "." + KEY_ACE_USER, ONE));
        coll.createIndex(new BasicDBObject(KEY_ACP + "." + KEY_ACL + "." + KEY_ACE_STATUS, ONE));
        // TODO configure these from somewhere else
        coll.createIndex(new BasicDBObject("dc:modified", MINUS_ONE));
        coll.createIndex(new BasicDBObject("rend:renditionName", ONE));
        coll.createIndex(new BasicDBObject("drv:subscriptions.enabled", ONE));
        coll.createIndex(new BasicDBObject("collectionMember:collectionIds", ONE));
        if (!isFulltextSearchDisabled()) {
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
        DBObject ob = stateToBson(state);
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: CREATE " + ob.get(idKey) + ": " + ob);
        }
        coll.insert(ob);
        // TODO dupe exception
        // throw new DocumentException("Already exists: " + id);
    }

    @Override
    public void createStates(List<State> states) {
        List<DBObject> obs = states.stream().map(this::stateToBson).collect(Collectors.toList());
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
    public State readPartialState(String id, Collection<String> keys) {
        DBObject query = new BasicDBObject(idKey, id);
        return findOne(query, keys);
    }

    @Override
    public List<State> readStates(List<String> ids) {
        DBObject query = new BasicDBObject(idKey, new BasicDBObject(QueryOperators.IN, ids));
        return findAll(query, ids.size());
    }

    @Override
    public void updateState(String id, StateDiff diff) {
        DBObject query = new BasicDBObject(idKey, id);
        for (DBObject update : diffToBson(diff)) {
            if (log.isTraceEnabled()) {
                log.trace("MongoDB: UPDATE " + id + ": " + update);
            }
            coll.update(query, update);
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
            DBObject notInIds = new BasicDBObject(QueryOperators.NIN, new ArrayList<String>(ignored));
            query.put(idKey, notInIds);
        }
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        DBObject query = new BasicDBObject(keyToBson(key), value);
        addIgnoredIds(query, ignored);
        return findAll(query, 0);
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        DBObject query = new BasicDBObject(keyToBson(key1), value1);
        query.put(keyToBson(key2), value2);
        addIgnoredIds(query, ignored);
        return findAll(query, 0);
    }

    @Override
    public void queryKeyValueArray(String key, Object value, Set<String> ids, Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies) {
        queryKeyValueArray(key, value, ids, proxyTargets, targetProxies, 0);
    }

    @Override
    public void queryKeyValueArray(String key, Object value, Set<String> ids, Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies, int limit) {
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
        DBCursor cursor = coll.find(query, fields).limit(limit);
        try {
            for (DBObject ob : cursor) {
                String id = (String) ob.get(idKey);
                ids.add(id);
                if (proxyTargets != null && TRUE.equals(ob.get(KEY_IS_PROXY))) {
                    String targetId = (String) ob.get(KEY_PROXY_TARGET_ID);
                    proxyTargets.put(id, targetId);
                }
                if (targetProxies != null) {
                    Object[] proxyIds = (Object[]) bsonToValue(ob.get(KEY_PROXY_IDS));
                    if (proxyIds != null) {
                        targetProxies.put(id, proxyIds);
                    }
                }
            }
        } finally {
            cursor.close();
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
        return bsonToState(coll.findOne(query));
    }

    protected State findOne(DBObject query, Collection<String> keys) {
        DBObject fields = new BasicDBObject(keys.size());
        keys.forEach(key -> fields.put(keyToBson(key), ONE));
        if (log.isTraceEnabled()) {
            logQuery(query, fields);
        }
        return bsonToState(coll.findOne(query, fields));
    }

    protected List<State> findAll(DBObject query, int sizeHint) {
        if (log.isTraceEnabled()) {
            logQuery(query, null);
        }
        DBCursor cursor = coll.find(query);
        Set<String> seen = new HashSet<>();
        try {
            List<State> list = new ArrayList<>(sizeHint);
            for (DBObject ob : cursor) {
                if (!seen.add((String) ob.get(idKey))) {
                    // MongoDB cursors may return the same
                    // object several times
                    continue;
                }
                list.add(bsonToState(ob));
            }
            return list;
        } finally {
            cursor.close();
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
        if (builder.hasFulltext && isFulltextSearchDisabled()) {
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
        DBCursor cursor = coll.find(query, keys).skip(offset).limit(limit);
        try {
            if (orderBy != null) {
                cursor.sort(orderBy);
            }
            projections = new ArrayList<>();
            DBSStateFlattener flattener = new DBSStateFlattener(builder.propertyKeys);
            for (DBObject ob : cursor) {
                State state = bsonToState(ob);
                if (manualProjection) {
                    projections.addAll(evaluator.matches(state));
                } else {
                    projections.add(flattener.flatten(state));
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
        } finally {
            cursor.close();
        }
        if (log.isTraceEnabled() && projections.size() != 0) {
            log.trace("MongoDB:    -> " + projections.size());
        }
        return new PartialList<>(projections, totalSize);
    }

    @Override
    public ScrollResult scroll(DBSExpressionEvaluator evaluator, int batchSize, int keepAliveSeconds) {
        checkForTimedoutScroll();
        MongoDBQueryBuilder builder = new MongoDBQueryBuilder(this, evaluator.getExpression(),
                evaluator.getSelectClause(), null, evaluator.pathResolver, evaluator.fulltextSearchDisabled);
        builder.walk();
        if (builder.hasFulltext && isFulltextSearchDisabled()) {
            throw new QueryParseException("Fulltext search disabled by configuration");
        }
        DBObject query = builder.getQuery();
        DBObject keys = builder.getProjection();
        if (log.isTraceEnabled()) {
            logQuery(query, keys, null, 0, 0);
        }

        DBCursor cursor = coll.find(query, keys);
        String scrollId = UUID.randomUUID().toString();
        registerCursor(scrollId, cursor, batchSize, keepAliveSeconds);
        return scroll(scrollId);
    }

    protected void checkForTimedoutScroll() {
        cursorResults.forEach((id, cursor) -> cursor.timedOut(id));
    }

    protected void registerCursor(String scrollId, DBCursor cursor, int batchSize, int keepAliveSeconds) {
        cursorResults.put(scrollId, new CursorResult(cursor, batchSize, keepAliveSeconds));
    }

    @Override
    public ScrollResult scroll(String scrollId) {
        CursorResult cursorResult = cursorResults.get(scrollId);
        if (cursorResult == null) {
            throw new NuxeoException("Unknown or timed out scrollId");
        } else if (cursorResult.timedOut(scrollId)) {
            throw new NuxeoException("Timed out scrollId");
        }
        cursorResult.touch();
        List<String> ids = new ArrayList<>(cursorResult.batchSize);
        synchronized (cursorResult) {
            if (cursorResult.cursor == null || !cursorResult.cursor.hasNext()) {
                unregisterCursor(scrollId);
                return emptyResult();
            }
            while (ids.size() < cursorResult.batchSize) {
                if (cursorResult.cursor == null || !cursorResult.cursor.hasNext()) {
                    cursorResult.close();
                    break;
                } else {
                    DBObject ob = cursorResult.cursor.next();
                    String id = (String) ob.get(keyToBson(KEY_ID));
                    if (id != null) {
                        ids.add(id);
                    } else {
                        log.error("Got a document without id: " + ob);
                    }
                }
            }
        }
        return new ScrollResultImpl(scrollId, ids);
    }

    protected boolean unregisterCursor(String scrollId) {
        CursorResult cursor = cursorResults.remove(scrollId);
        if (cursor != null) {
            cursor.close();
            return true;
        }
        return false;
    }

    protected void addPrincipals(DBObject query, Set<String> principals) {
        if (principals != null) {
            DBObject inPrincipals = new BasicDBObject(QueryOperators.IN, new ArrayList<String>(principals));
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
        BlobManager blobManager = Framework.getService(BlobManager.class);
        // TODO add a query to not scan all documents
        if (log.isTraceEnabled()) {
            logQuery(new BasicDBObject(), binaryKeys);
        }
        DBCursor cursor = coll.find(new BasicDBObject(), binaryKeys);
        try {
            for (DBObject ob : cursor) {
                markReferencedBinaries(ob, blobManager);
            }
        } finally {
            cursor.close();
        }
    }

    protected void markReferencedBinaries(DBObject ob, BlobManager blobManager) {
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

    protected void markReferencedBinary(Object value, BlobManager blobManager) {
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
        Calendar created = (Calendar) scalarToSerializable(res.get(KEY_LOCK_CREATED));
        return new Lock(owner, created);
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        DBObject query = new BasicDBObject(idKey, id);
        query.put(KEY_LOCK_OWNER, null); // select doc if no lock is set
        DBObject setLock = new BasicDBObject();
        setLock.put(KEY_LOCK_OWNER, lock.getOwner());
        setLock.put(KEY_LOCK_CREATED, serializableToBson(lock.getCreated()));
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
            Calendar oldCreated = (Calendar) scalarToSerializable(old.get(KEY_LOCK_CREATED));
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
                Calendar oldCreated = (Calendar) scalarToSerializable(old.get(KEY_LOCK_CREATED));
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
            Calendar oldCreated = (Calendar) scalarToSerializable(old.get(KEY_LOCK_CREATED));
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

    protected class CursorResult {
        // Note that MongoDB cursor automatically timeout after 10 minutes of inactivity by default.
        protected DBCursor cursor;
        protected final int batchSize;
        protected long lastCallTimestamp;
        protected final int keepAliveSeconds;

        public CursorResult(DBCursor cursor, int batchSize, int keepAliveSeconds) {
            this.cursor = cursor;
            this.batchSize = batchSize;
            this.keepAliveSeconds = keepAliveSeconds;
            lastCallTimestamp = System.currentTimeMillis();
        }

        void touch() {
            lastCallTimestamp = System.currentTimeMillis();
        }

        boolean timedOut(String scrollId) {
            long now = System.currentTimeMillis();
            if (now - lastCallTimestamp > (keepAliveSeconds * 1000)) {
                if (unregisterCursor(scrollId)) {
                    log.warn("Scroll " + scrollId + " timed out");
                }
                return true;
            }
            return false;
        }

        public void close() {
            if (cursor != null) {
                cursor.close();
            }
            cursor = null;
        }
    }
}
