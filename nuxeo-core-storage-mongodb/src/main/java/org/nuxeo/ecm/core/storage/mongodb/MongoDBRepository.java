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
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_BINARY;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.Diff;
import org.nuxeo.ecm.core.storage.State.DiffOp;
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

    public static final String MONGODB_POP = "$pop";

    private static final String MONGODB_INDEX_TEXT = "text";

    private static final String MONGODB_INDEX_NAME = "name";

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

    /**
     * Constructs a MongoDB update from the given {@link Diff}.
     */
    protected DBObject diffToBson(Diff diff) {
        BasicDBObject set = new BasicDBObject();
        BasicDBObject unset = new BasicDBObject();
        BasicDBObject push = new BasicDBObject();
        BasicDBObject pop = new BasicDBObject();
        diffToUpdate(diff, "", set, unset, push, pop);
        DBObject update = new BasicDBObject();
        if (!set.isEmpty()) {
            update.put(MONGODB_SET, set);
        }
        if (!unset.isEmpty()) {
            update.put(MONGODB_UNSET, unset);
        }
        if (!push.isEmpty()) {
            update.put(MONGODB_PUSH, push);
        }
        if (!pop.isEmpty()) {
            update.put(MONGODB_POP, pop);
        }
        return update;
    }

    protected void diffToUpdate(Diff diff, String prefix, DBObject set,
            DBObject unset, DBObject push, DBObject pop) {
        for (Entry<String, Serializable> en : diff.entrySet()) {
            String name = prefix + '.' + en.getKey();
            Serializable value = en.getValue();
            if (value instanceof Diff) {
                diffToUpdate((Diff) value, name, set, unset, push, pop);
            } else if (value instanceof Object[]) {
                Object[] array = (Object[]) value;
                diffOrListToUpdate(Arrays.asList(array), name, set, push, pop);
            } else if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                diffOrListToUpdate(list, name, set, push, pop);
            } else {
                // not a diff
                set.put(name, valueToBson(value));
            }
        }
    }

    protected void diffOrListToUpdate(List<Object> list, String name,
            DBObject set, DBObject push, DBObject pop) throws AssertionError {
        if (!list.isEmpty() && list.get(0) instanceof DiffOp) {
            // list diff
            DiffOp op = (DiffOp) list.get(0);
            switch (op) {
            case RPUSH:
                Object pushed;
                if (list.size() == 2) {
                    // no need to use $each for one element
                    pushed = valueToBson(list.get(1));
                } else {
                    List<Object> values = new ArrayList<Object>(list.size() - 1);
                    for (int i = 1; i < list.size(); i++) {
                        values.add(valueToBson(list.get(i)));
                    }
                    pushed = new BasicDBObject(MONGODB_EACH, values);
                }
                push.put(name, pushed);
                break;
            case RPOP:
                pop.put(name, ONE);
                break;
            default:
                throw new AssertionError(op.toString());
            }
        } else {
            // not a diff
            set.put(name, valueToBson(list));
        }
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
        DBObject indexOptions = new BasicDBObject(MONGODB_INDEX_NAME,
                "fulltext");
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
    public void updateState(String id, Diff diff) throws DocumentException {
        DBObject query = new BasicDBObject(KEY_ID, id);
        DBObject update = diffToBson(diff);
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: UPDATE " + id + ": " + update);
        }
        coll.update(query, update);
        // TODO dupe exception
        // throw new DocumentException("Missing: " + id);
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
        try {
            List<State> list = new ArrayList<>(sizeHint);
            for (DBObject ob : cursor) {
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
            int limit, int offset, int countUpTo, boolean deepCopy) {
        MongoDBQueryBuilder builder = new MongoDBQueryBuilder(
                evaluator.pathResolver);
        DBObject query = builder.walkExpression(expression);
        addPrincipals(query, evaluator.principals);
        List<State> list;
        if (log.isTraceEnabled()) {
            log.trace("MongoDB: QUERY " + query + " OFFSET " + offset
                    + " LIMIT " + limit);
        }
        long totalSize;
        DBCursor cursor = coll.find(query).skip(offset).limit(limit);
        try {
            if (orderByClause != null) {
                DBObject orderBy = new BasicDBObject();
                for (OrderByExpr ob : orderByClause.elements) {
                    Reference ref = ob.reference;
                    boolean desc = ob.isDescending;
                    String field = builder.walkReference(ref).field;
                    if (!orderBy.containsField(field)) {
                        orderBy.put(field, desc ? MINUS_ONE : ONE);
                    }
                }
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
            log.trace("MongoDB: -> " + list.size());
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
