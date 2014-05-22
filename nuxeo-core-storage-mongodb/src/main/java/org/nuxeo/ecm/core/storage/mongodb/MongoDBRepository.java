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
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_IS_PROXY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_IDS;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PROXY_TARGET_ID;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.query.sql.model.OrderByExpr;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;

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

    private static final Long ZERO = Long.valueOf(0);

    private static final Long ONE = Long.valueOf(1);

    private static final Long MINUS_ONE = Long.valueOf(-1);

    public static final String DB_NAME = "nuxeo";

    public static final String MONGO_ID = "_id";

    protected MongoClient mongoClient;

    protected DB db;

    protected DBCollection coll;

    public MongoDBRepository(String repositoryName) {
        super(repositoryName);
        try {
            // TODO host, port, sharding options
            mongoClient = new MongoClient();
            // TODO mongoClient.setWriteConcern
            // TODO configure db name
            // TODO authentication
            db = mongoClient.getDB(DB_NAME);
            coll = db.getCollection(repositoryName);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        initRoot();
    }

    @Override
    public void shutdown() {
        mongoClient.close();
    }

    protected DBObject stateToBson(Map<String, Serializable> state,
            boolean skipNull) {
        DBObject ob = new BasicDBObject();
        for (Entry<String, Serializable> en : state.entrySet()) {
            String key = en.getKey();
            Serializable value = en.getValue();
            Object val;
            if (value instanceof Map) {
                val = stateToBson((Map<String, Serializable>) value, skipNull);
            } else if (value instanceof List) {
                List<Serializable> states = (List<Serializable>) value;
                ArrayList<DBObject> obs = new ArrayList<DBObject>(states.size());
                for (Serializable state1 : states) {
                    obs.add(stateToBson((Map<String, Serializable>) state1,
                            skipNull));
                }
                val = obs;
            } else if (value instanceof Object[]) {
                Object[] ar = (Object[]) value;
                List<Object> list = new ArrayList<>(ar.length);
                for (Object v : ar) {
                    list.add(serializableToScalar((Serializable) v));
                }
                val = list;
            } else {
                val = serializableToScalar(value);
            }
            if (val != null || !skipNull) {
                ob.put(key, val);
            }
        }
        return ob;
    }

    protected Map<String, Serializable> bsonToState(DBObject ob) {
        if (ob == null) {
            return null;
        }
        Map<String, Serializable> state = new HashMap<>();
        for (String key : ob.keySet()) {
            Object val = ob.get(key);
            Serializable value;
            if (val instanceof List) {
                List<Object> list = (List<Object>) val;
                if (list.isEmpty()) {
                    value = null;
                } else {
                    if (list.get(0) instanceof DBObject) {
                        List<Serializable> l = new ArrayList<>(list.size());
                        for (Object el : list) {
                            l.add((Serializable) bsonToState((DBObject) el));
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
                value = (Serializable) bsonToState((DBObject) val);
            } else {
                if (MONGO_ID.equals(key)) {
                    // skip ObjectId
                    continue;
                }
                value = scalarToSerializable(val);
            }
            state.put(key, value);
        }
        return state;
    }

    protected Object serializableToScalar(Serializable value) {
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

    @Override
    public void createState(Map<String, Serializable> state)
            throws DocumentException {
        DBObject ob = stateToBson(state, true);
        coll.insert(ob);
        // TODO dupe exception
        // throw new DocumentException("Already exists: " + id);
    }

    @Override
    public Map<String, Serializable> readState(String id) {
        DBObject query = new BasicDBObject(KEY_ID, id);
        return findOne(query);
    }

    @Override
    public List<Map<String, Serializable>> readStates(List<String> ids) {
        DBObject query = new BasicDBObject(KEY_ID, new BasicDBObject(
                QueryOperators.IN, ids));
        return findAll(query, ids.size());
    }

    @Override
    public void updateState(Map<String, Serializable> state)
            throws DocumentException {
        String id = (String) state.get(KEY_ID);
        DBObject query = new BasicDBObject(KEY_ID, id);
        DBObject ob = stateToBson(state, false);
        coll.update(query, ob);
        // TODO dupe exception
        // throw new DocumentException("Missing: " + id);
    }

    @Override
    public void deleteState(String id) throws DocumentException {
        DBObject query = new BasicDBObject(KEY_ID, id);
        WriteResult w = coll.remove(query);
        log.error("XXX DEBUG removed N=" + w.getN());
    }

    @Override
    public Map<String, Serializable> readChildState(String parentId,
            String name, Set<String> ignored) {
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
    public List<Map<String, Serializable>> queryKeyValue(String key,
            String value, Set<String> ignored) {
        DBObject query = new BasicDBObject(key, value);
        addIgnoredIds(query, ignored);
        return findAll(query, 0);
    }

    @Override
    public void queryKeyValueArray(String key, Object value, Set<String> ids,
            Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies, Set<String> ignored) {
        DBObject query = new BasicDBObject(key, value);
        addIgnoredIds(query, ignored);
        DBObject fields = new BasicDBObject();
        fields.put(MONGO_ID, ZERO);
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

    protected Map<String, Serializable> findOne(DBObject query) {
        return bsonToState(coll.findOne(query));
    }

    protected List<Map<String, Serializable>> findAll(DBObject query,
            int sizeHint) {
        DBCursor cursor = coll.find(query);
        try {
            List<Map<String, Serializable>> list = new ArrayList<>(sizeHint);
            for (DBObject ob : cursor) {
                list.add(bsonToState(ob));
            }
            return list;
        } finally {
            cursor.close();
        }
    }

    protected DBObject justPresenceField() {
        return new BasicDBObject(MONGO_ID, ONE);
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(
            Expression expression, DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, int limit, int offset, int countUpTo,
            boolean deepCopy, Set<String> ignored) {
        MongoDBQueryBuilder builder = new MongoDBQueryBuilder(
                evaluator.pathResolver);
        DBObject query = builder.walkExpression(expression);
        addIgnoredIds(query, ignored);
        List<Map<String, Serializable>> list;
        System.err.println(query); // XXX
        DBCursor cursor = coll.find(query).skip(offset).limit(limit);
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
        long totalSize;
        try {
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
        return new PartialList<>(list, totalSize);
    }

}
