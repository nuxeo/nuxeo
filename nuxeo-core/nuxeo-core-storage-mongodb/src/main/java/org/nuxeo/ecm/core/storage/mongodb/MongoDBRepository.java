/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.mongodb;

import static java.lang.Boolean.TRUE;
import static org.nuxeo.ecm.core.storage.State.NOP;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_BINARY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_SCORE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_SIMPLE;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_PROXY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_TARGET_ID;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.ListDiff;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
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

    private static final Long ZERO = Long.valueOf(0);

    private static final Long ONE = Long.valueOf(1);

    private static final Long MINUS_ONE = Long.valueOf(-1);

    public static final String DB_NAME = "nuxeo";

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

    public MongoDBRepository(MongoDBRepositoryDescriptor descriptor) {
        super(descriptor.name);
        try {
            mongoClient = newMongoClient(descriptor);
            coll = getCollection(descriptor, mongoClient);
            countersColl = getCountersCollection(descriptor, mongoClient);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        initRepository();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        mongoClient.close();
    }

    // used also by unit tests
    public static MongoClient newMongoClient(
            MongoDBRepositoryDescriptor descriptor) throws UnknownHostException {
        ServerAddress addr = new ServerAddress(descriptor.server);
        // TODO sharding options
        // TODO mongoClient.setWriteConcern
        return new MongoClient(addr);
    }

    protected static DBCollection getCollection(MongoClient mongoClient,
            String name) {
        // TODO configure db name
        // TODO authentication
        DB db = mongoClient.getDB(DB_NAME);
        return db.getCollection(name);
    }

    // used also by unit tests
    public static DBCollection getCollection(
            MongoDBRepositoryDescriptor descriptor, MongoClient mongoClient) {
        return getCollection(mongoClient, descriptor.name);
    }

    // used also by unit tests
    public static DBCollection getCountersCollection(
            MongoDBRepositoryDescriptor descriptor, MongoClient mongoClient) {
        return getCollection(mongoClient, descriptor.name + ".counters");
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
                ob.put(en.getKey(), val);
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

    protected State bsonToState(DBObject ob) {
        if (ob == null) {
            return null;
        }
        State state = new State(ob.keySet().size());
        for (String key : ob.keySet()) {
            Object val = ob.get(key);
            Serializable value;
            if (val instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) val;
                if (list.isEmpty()) {
                    value = null;
                } else {
                    if (list.get(0) instanceof DBObject) {
                        List<Serializable> l = new ArrayList<>(list.size());
                        for (Object el : list) {
                            l.add(bsonToState((DBObject) el));
                        }
                        value = (Serializable) l;
                    } else {
                        Object[] ar = new Object[list.size()];
                        int i = 0;
                        for (Object el : list) {
                            ar[i++] = scalarToSerializable(el);
                        }
                        value = ar;
                    }
                }
            } else if (val instanceof DBObject) {
                value = bsonToState((DBObject) val);
            } else {
                if (MONGODB_ID.equals(key)) {
                    // skip ObjectId
                    continue;
                }
                value = scalarToSerializable(val);
            }
            state.put(key, value);
        }
        return state;
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
     * This happens if two operations act on two fields where one is a prefix of
     * the other.
     * <p>
     * Example: Cannot update 'mylist.0.string' and 'mylist' at the same time
     * (error 16837)
     *
     * @since 5.9.5
     */
    protected static class UpdateListBuilder {

        protected List<DBObject> updateList = new ArrayList<>(1);

        protected DBObject update;

        protected List<String> keys;

        protected UpdateListBuilder() {
            newUpdate();
        }

        protected void newUpdate() {
            updateList.add(update = new BasicDBObject());
            keys = new ArrayList<>();
        }

        protected void update(String op, String key, Object value) {
            if (conflicts(key, keys)) {
                newUpdate();
            }
            keys.add(key);
            DBObject map = (DBObject) update.get(op);
            if (map == null) {
                update.put(op, map = new BasicDBObject());
            }
            map.put(key, value);
        }

        /**
         * Checks if the key conflicts with one of the previous keys.
         * <p>
         * A conflict occurs if one key is equals to or is a prefix of the
         * other.
         */
        protected boolean conflicts(String key, List<String> previousKeys) {
            String keydot = key + '.';
            for (String prev : previousKeys) {
                if (prev.equals(key) || prev.startsWith(keydot)
                        || key.startsWith(prev + '.')) {
                    return true;
                }
            }
            return false;
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
                updates.set.put(name, valueToBson(value));
            }
        }
    }

    protected void diffToUpdates(ListDiff listDiff, String prefix,
            Updates updates) {
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
                pushed = new BasicDBObject(MONGODB_EACH,
                        listToBson(listDiff.rpush));
            }
            updates.push.put(prefix, pushed);
        }
    }

    protected void diffToUpdates(Delta delta, String prefix, Updates updates) {
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

    protected void initRepository() {
        // create required indexes
        DBObject indexKeys = new BasicDBObject();
        indexKeys.put(KEY_FULLTEXT_SIMPLE, MONGODB_INDEX_TEXT);
        indexKeys.put(KEY_FULLTEXT_BINARY, MONGODB_INDEX_TEXT);
        DBObject indexOptions = new BasicDBObject();
        indexOptions.put(MONGODB_INDEX_NAME, FULLTEXT_INDEX_NAME);
        indexOptions.put(MONGODB_LANGUAGE_OVERRIDE, LANGUAGE_FIELD);
        coll.createIndex(indexKeys, indexOptions);
        // check root presence
        DBObject query = new BasicDBObject(KEY_ID, getRootId());
        if (coll.findOne(query, justPresenceField()) != null) {
            return;
        }
        // create basic repository structure needed
        if (DEBUG_UUIDS) {
            // create the id counter
            DBObject idCounter = new BasicDBObject();
            idCounter.put(MONGODB_ID, COUNTER_NAME_UUID);
            idCounter.put(COUNTER_FIELD, ZERO);
            countersColl.insert(idCounter);
        }
        initRoot();
    }

    protected Long getNextUuidSeq() {
        DBObject query = new BasicDBObject(MONGODB_ID, COUNTER_NAME_UUID);
        DBObject update = new BasicDBObject(MONGODB_INC, new BasicDBObject(
                COUNTER_FIELD, ONE));
        boolean returnNew = true;
        DBObject idCounter = countersColl.findAndModify(query, null, null,
                false, update, returnNew, false);
        if (idCounter == null) {
            throw new RuntimeException("Repository id counter not initialized");
        }
        return (Long) idCounter.get(COUNTER_FIELD);
    }

    @Override
    public String generateNewId() {
        if (DEBUG_UUIDS) {
            Long id = getNextUuidSeq();
            return "UUID_" + id;
        } else {
            return UUID.randomUUID().toString();
        }
    }

    @Override
    public void createState(State state) throws DocumentException {
        DBObject ob = stateToBson(state);
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: CREATE " + ob);
        }
        coll.insert(ob);
        // TODO dupe exception
        // throw new DocumentException("Already exists: " + id);
    }

    @Override
    public State readState(String id) {
        DBObject query = new BasicDBObject(KEY_ID, id);
        return findOne(query);
    }

    @Override
    public List<State> readStates(List<String> ids) {
        DBObject query = new BasicDBObject(KEY_ID, new BasicDBObject(
                QueryOperators.IN, ids));
        return findAll(query, ids.size());
    }

    @Override
    public void updateState(String id, StateDiff diff) throws DocumentException {
        DBObject query = new BasicDBObject(KEY_ID, id);
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
    public void deleteStates(Set<String> ids) throws DocumentException {
        DBObject query = new BasicDBObject(KEY_ID, new BasicDBObject(
                QueryOperators.IN, ids));
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: REMOVE " + ids);
        }
        WriteResult w = coll.remove(query);
        if (w.getN() != ids.size()) {
            log.error("Removed " + w.getN() + " docs for " + ids.size()
                    + " ids: " + ids);
        }
    }

    @Override
    public State readChildState(String parentId, String name,
            Set<String> ignored) {
        DBObject query = getChildQuery(parentId, name, ignored);
        return findOne(query);
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        DBObject query = getChildQuery(parentId, name, ignored);
        return coll.findOne(query, justPresenceField()) != null;
    }

    protected DBObject getChildQuery(String parentId, String name,
            Set<String> ignored) {
        DBObject query = new BasicDBObject();
        query.put(KEY_PARENT_ID, parentId);
        query.put(KEY_NAME, name);
        addIgnoredIds(query, ignored);
        return query;
    }

    protected void addIgnoredIds(DBObject query, Set<String> ignored) {
        if (!ignored.isEmpty()) {
            DBObject notInIds = new BasicDBObject(QueryOperators.NIN,
                    new ArrayList<String>(ignored));
            query.put(KEY_ID, notInIds);
        }
    }

    @Override
    public List<State> queryKeyValue(String key, String value,
            Set<String> ignored) {
        DBObject query = new BasicDBObject(key, value);
        addIgnoredIds(query, ignored);
        return findAll(query, 0);
    }

    @Override
    public void queryKeyValueArray(String key, Object value, Set<String> ids,
            Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies) {
        DBObject query = new BasicDBObject(key, value);
        DBObject fields = new BasicDBObject();
        fields.put(MONGODB_ID, ZERO);
        fields.put(KEY_ID, ONE);
        fields.put(KEY_IS_PROXY, ONE);
        fields.put(KEY_PROXY_TARGET_ID, ONE);
        fields.put(KEY_PROXY_IDS, ONE);
        DBCursor cursor = coll.find(query, fields);
        try {
            for (DBObject ob : cursor) {
                String id = (String) ob.get(KEY_ID);
                ids.add(id);
                if (proxyTargets != null && TRUE.equals(ob.get(KEY_IS_PROXY))) {
                    String targetId = (String) ob.get(KEY_PROXY_TARGET_ID);
                    proxyTargets.put(id, targetId);
                }
                if (targetProxies != null) {
                    Object[] proxyIds = (Object[]) ob.get(KEY_PROXY_IDS);
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
    public boolean queryKeyValuePresence(String key, String value,
            Set<String> ignored) {
        DBObject query = new BasicDBObject(key, value);
        addIgnoredIds(query, ignored);
        return coll.findOne(query, justPresenceField()) != null;
    }

    protected State findOne(DBObject query) {
        return bsonToState(coll.findOne(query));
    }

    protected List<State> findAll(DBObject query, int sizeHint) {
        DBCursor cursor = coll.find(query);
        Set<String> seen = new HashSet<>();
        try {
            List<State> list = new ArrayList<>(sizeHint);
            for (DBObject ob : cursor) {
                if (!seen.add((String) ob.get(KEY_ID))) {
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
    public PartialList<State> queryAndFetch(Expression expression,
            DBSExpressionEvaluator evaluator, OrderByClause orderByClause,
            int limit, int offset, int countUpTo, boolean deepCopy,
            boolean fulltextScore) {
        MongoDBQueryBuilder builder = new MongoDBQueryBuilder(
                evaluator.pathResolver);
        DBObject query = builder.walkExpression(expression);
        addPrincipals(query, evaluator.principals);

        // order by

        BasicDBObject orderBy;
        boolean sortScore = false;
        if (orderByClause == null) {
            orderBy = null;
        } else {
            orderBy = new BasicDBObject();
            for (OrderByExpr ob : orderByClause.elements) {
                Reference ref = ob.reference;
                boolean desc = ob.isDescending;
                String field = builder.walkReference(ref).field;
                if (!orderBy.containsField(field)) {
                    Object value;
                    if (KEY_FULLTEXT_SCORE.equals(field)) {
                        if (!desc) {
                            throw new RuntimeException("Cannot sort by "
                                    + NXQL.ECM_FULLTEXT_SCORE + " ascending");
                        }
                        sortScore = true;
                        value = new BasicDBObject(MONGODB_META,
                                MONGODB_TEXT_SCORE);
                    } else {
                        value = desc ? MINUS_ONE : ONE;
                    }
                    orderBy.put(field, value);
                }
            }
            if (sortScore && orderBy.size() > 1) {
                throw new RuntimeException("Cannot sort by "
                        + NXQL.ECM_FULLTEXT_SCORE + " and other criteria");
            }
        }

        // projection

        DBObject keys;
        if (fulltextScore || sortScore) {
            if (!builder.hasFulltext) {
                throw new RuntimeException(NXQL.ECM_FULLTEXT_SCORE
                        + " cannot be used without " + NXQL.ECM_FULLTEXT);
            }
            // because it's a $meta, it won't prevent all other keys
            // from being returned
            keys = new BasicDBObject(KEY_FULLTEXT_SCORE, new BasicDBObject(
                    MONGODB_META, MONGODB_TEXT_SCORE));
        } else {
            keys = null; // all
        }

        if (log.isTraceEnabled()) {
            log.trace("MongoDB: QUERY " + query
                    + (orderBy == null ? "" : " ORDER BY " + orderBy)
                    + " OFFSET " + offset + " LIMIT " + limit);
        }

        List<State> list;
        long totalSize;
        DBCursor cursor = coll.find(query, keys).skip(offset).limit(limit);
        try {
            if (orderBy != null) {
                cursor = cursor.sort(orderBy);
            }
            list = new ArrayList<>();
            for (DBObject ob : cursor) {
                list.add(bsonToState(ob));
            }
            if (countUpTo == -1) {
                // count full size
                if (limit == 0) {
                    totalSize = list.size();
                } else {
                    totalSize = cursor.count();
                }
            } else if (countUpTo == 0) {
                // no count
                totalSize = -1; // not counted
            } else {
                // count only if less than countUpTo
                if (limit == 0) {
                    totalSize = list.size();
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
        if (log.isTraceEnabled() && list.size() != 0) {
            log.trace("MongoDB:    -> " + list.size());
        }
        return new PartialList<>(list, totalSize);
    }

    protected void addPrincipals(DBObject query, Set<String> principals) {
        if (principals != null) {
            DBObject inPrincipals = new BasicDBObject(QueryOperators.IN,
                    new ArrayList<String>(principals));
            query.put(DBSDocument.KEY_READ_ACL, inPrincipals);
        }
    }

}
