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

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSSession.TYPE_ROOT;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepository;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.DefaultBinaryManager;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;

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
public class MongoDBRepository implements DBSRepository {

    private static final Log log = LogFactory.getLog(MongoDBRepository.class);

    public static final String DB_NAME = "nuxeo";

    // change to have deterministic pseudo-UUID generation for debugging
    private final boolean DEBUG_UUIDS = true;

    // for debug
    private final AtomicLong temporaryIdCounter = new AtomicLong(0);

    protected final String repositoryName;

    protected final BinaryManager binaryManager;

    protected MongoClient mongoClient;

    protected DB db;

    protected DBCollection coll;

    public MongoDBRepository(String repositoryName) {
        this.repositoryName = repositoryName;
        binaryManager = newBinaryManager();
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
        initRootACP();
    }

    // TODO factor out
    protected void initRootACP() {
        try {
            DBSSession session = getSession(null);
            Document root = session.addChild(getRootId(), null, "", null,
                    TYPE_ROOT);
            ACLImpl acl = new ACLImpl();
            acl.add(new ACE(SecurityConstants.ADMINISTRATORS,
                    SecurityConstants.EVERYTHING, true));
            acl.add(new ACE(SecurityConstants.ADMINISTRATOR,
                    SecurityConstants.EVERYTHING, true));
            acl.add(new ACE(SecurityConstants.MEMBERS, SecurityConstants.READ,
                    true));
            ACPImpl acp = new ACPImpl();
            acp.addACL(acl);
            session.setACP(root, acp, true);
            session.commit();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    protected BinaryManager newBinaryManager() {
        BinaryManager binaryManager = new DefaultBinaryManager();
        RepositoryDescriptor repositoryDescriptor = new RepositoryDescriptor();
        try {
            File dir = File.createTempFile("memBinaryManager", "");
            dir.delete();
            repositoryDescriptor.name = "mem";
            repositoryDescriptor.binaryStorePath = dir.getPath();
            binaryManager.initialize(repositoryDescriptor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return binaryManager;
    }

    @Override
    public String getName() {
        return repositoryName;
    }

    @Override
    public void shutdown() {
        mongoClient.close();
    }

    @Override
    public int getActiveSessionsCount() {
        return 0;
    }

    @Override
    public DBSSession getSession(String sessionId) throws DocumentException {
        return new DBSSession(this, sessionId);
    }

    @Override
    public BinaryManager getBinaryManager() {
        return binaryManager;
    }

    @Override
    public String getRootId() {
        if (DEBUG_UUIDS) {
            return "UUID_0";
        } else {
            return "00000000-0000-0000-0000-000000000000";
        }
    }

    @Override
    public String generateNewId() {
        if (DEBUG_UUIDS) {
            return "UUID_" + temporaryIdCounter.incrementAndGet();
        } else {
            return UUID.randomUUID().toString();
        }
    }

    protected DBObject stateToBson(Map<String, Serializable> state) {
        DBObject ob = new BasicDBObject();
        for (Entry<String, Serializable> en : state.entrySet()) {
            String key = en.getKey();
            Serializable value = en.getValue();
            Object val;
            if (value instanceof Map) {
                val = stateToBson((Map<String, Serializable>) value);
            } else if (value instanceof List) {
                List<Serializable> states = (List<Serializable>) value;
                ArrayList<DBObject> obs = new ArrayList<DBObject>(states.size());
                for (Serializable state1 : states) {
                    obs.add(stateToBson((Map<String, Serializable>) state1));
                }
                val = obs;
            } else if (value instanceof Object[]) {
                val = Arrays.asList((Object[]) value);
            } else {
                val = serializableToScalar(value);
            }
            ob.put(key, val);
        }
        return ob;
    }

    protected Map<String, Serializable> bsonToState(DBObject ob) {
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
                if ("_id".equals(key)) {
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
        DBObject ob = stateToBson(state);
        coll.insert(ob);
        // TODO dupe exception
        // throw new DocumentException("Already exists: " + id);
    }

    @Override
    public Map<String, Serializable> readState(String id) {
        DBObject q = new BasicDBObject(KEY_ID, id);
        DBCursor cursor = coll.find(q).limit(2);
        try {
            List<DBObject> ar = cursor.toArray();
            if (ar.isEmpty()) {
                return null;
            } else if (ar.size() > 1) {
                log.error("Reading document " + id
                        + " returned more than one result");
            }
            return bsonToState(ar.get(0));
        } finally {
            cursor.close();
        }
    }

    @Override
    public List<Map<String, Serializable>> readStates(List<String> ids) {
        List<Map<String, Serializable>> list = new ArrayList<>();
        for (String id : ids) {
            list.add(readState(id));
        }
        return list;
    }

    @Override
    public void updateState(Map<String, Serializable> state)
            throws DocumentException {
        String id = (String) state.get(KEY_ID);
        DBObject q = new BasicDBObject(KEY_ID, id);
        DBObject ob = stateToBson(state);
        coll.update(q, ob);
        // TODO dupe exception
        // throw new DocumentException("Missing: " + id);
    }

    @Override
    public void deleteState(String id) throws DocumentException {
        DBObject q = new BasicDBObject(KEY_ID, id);
        WriteResult w = coll.remove(q);
        log.error("XXX DEBUG removed N=" + w.getN());
    }

    @Override
    public Map<String, Serializable> readChildState(String parentId,
            String name, Set<String> ignored) {
        DBObject q = new BasicDBObject();
        q.put(KEY_PARENT_ID, parentId);
        q.put(KEY_NAME, name);
        if (!ignored.isEmpty()) {
            DBObject notInIds = new BasicDBObject(QueryOperators.NIN,
                    new ArrayList<String>(ignored));
            q.put(KEY_ID, notInIds);
        }
        DBCursor cursor = coll.find(q).limit(2);
        try {
            List<DBObject> ar = cursor.toArray();
            if (ar.isEmpty()) {
                return null;
            } else if (ar.size() > 1) {
                log.error("Reading document child " + name + " of " + parentId
                        + " returned more than one result");
            }
            return bsonToState(ar.get(0));
        } finally {
            cursor.close();
        }
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        return readChildState(parentId, name, ignored) != null;
    }

    // TODO XXX add ignored set
    @Override
    public List<Map<String, Serializable>> readKeyValuedStates(String key,
            String value) {
        DBObject q = new BasicDBObject(key, value);
        // TODO ignored
        DBCursor cursor = coll.find(q);
        try {
            List<Map<String, Serializable>> list = new ArrayList<>();
            for (DBObject ob : cursor) {
                list.add(bsonToState(ob));
            }
            return list;
        } finally {
            cursor.close();
        }
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(
            DBSExpressionEvaluator evaluator, OrderByClause orderBy,
            long limit, long offset, boolean deepCopy, Set<String> ignored) {
        throw new UnsupportedOperationException("queryAndFetch");
    }

}
