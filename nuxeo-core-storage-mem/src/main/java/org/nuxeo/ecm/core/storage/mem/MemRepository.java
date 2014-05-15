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
package org.nuxeo.ecm.core.storage.mem;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSSession.TYPE_ROOT;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.CopyHelper;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.binary.BinaryManager;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerDescriptor;
import org.nuxeo.ecm.core.storage.binary.BinaryManagerService;
import org.nuxeo.ecm.core.storage.binary.DefaultBinaryManager;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator;
import org.nuxeo.ecm.core.storage.dbs.DBSExpressionEvaluator.OrderByComparator;
import org.nuxeo.ecm.core.storage.dbs.DBSRepository;
import org.nuxeo.ecm.core.storage.dbs.DBSSession;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * In-memory implementation of a {@link Repository}.
 * <p>
 * Internally, the repository is a map from id to document object.
 * <p>
 * A document object is a JSON-like document stored as a Map recursively
 * containing the data, see {@link DBSDocument} for the description of the
 * document.
 *
 * @since 5.9.4
 */
public class MemRepository implements DBSRepository {

    private static final Log log = LogFactory.getLog(MemRepository.class);

    // change to have deterministic pseudo-UUID generation for debugging
    private final boolean DEBUG_UUIDS = true;

    // for debug
    private final AtomicLong temporaryIdCounter = new AtomicLong(0);

    protected final String repositoryName;

    /**
     * The content of the repository, a map of document id -> object.
     */
    protected Map<String, Map<String, Serializable>> states;

    protected final BinaryManager binaryManager;

    public MemRepository(String repositoryName) {
        this.repositoryName = repositoryName;
        states = new HashMap<>();
        binaryManager = newBinaryManager();
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
        BinaryManagerDescriptor binaryManagerDescriptor = new BinaryManagerDescriptor();
        try {
            File dir = File.createTempFile("memBinaryManager", "");
            dir.delete();
            binaryManagerDescriptor.repositoryName = "mem";
            binaryManagerDescriptor.storePath = dir.getPath();
            binaryManager.initialize(binaryManagerDescriptor);
            BinaryManagerService bms = Framework.getLocalService(BinaryManagerService.class);
            bms.addBinaryManager(binaryManagerDescriptor.repositoryName, binaryManager);
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
        states = null;
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

    @Override
    public Map<String, Serializable> readState(String id) {
        Map<String, Serializable> state = states.get(id);
        // log.error("read   " + id + ": " + state);
        return state;
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
    public void createState(Map<String, Serializable> state)
            throws DocumentException {
        String id = (String) state.get(KEY_ID);
        // log.error("create " + id + ": " + state);
        if (states.containsKey(id)) {
            throw new DocumentException("Already exists: " + id);
        }
        states.put(id, state);
    }

    @Override
    public void updateState(Map<String, Serializable> state)
            throws DocumentException {
        String id = (String) state.get(KEY_ID);
        // log.error("update " + id + ": " + state);
        if (!states.containsKey(id)) {
            throw new DocumentException("Missing: " + id);
        }
        states.put(id, state);
    }

    @Override
    public void deleteState(String id) throws DocumentException {
        // log.error("delete " + id);
        if (states.remove(id) == null) {
            throw new DocumentException("Missing: " + id);
        }
    }

    @Override
    public Map<String, Serializable> readChildState(String parentId,
            String name, Set<String> ignored) {
        // TODO optimize by maintaining a parent/child index
        for (Map<String, Serializable> state : states.values()) {
            if (ignored.contains(state.get(KEY_ID))) {
                continue;
            }
            if (!parentId.equals(state.get(KEY_PARENT_ID))) {
                continue;
            }
            if (!name.equals(state.get(KEY_NAME))) {
                continue;
            }
            return state;
        }
        return null;
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        return readChildState(parentId, name, ignored) != null;
    }

    // TODO XXX add ignored set
    @Override
    public List<Map<String, Serializable>> readKeyValuedStates(String key,
            String value) {
        List<Map<String, Serializable>> list = new ArrayList<>();
        for (Map<String, Serializable> state : states.values()) {
            if (!value.equals(state.get(key))) {
                continue;
            }
            list.add(state);
        }
        return list;
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(
            Expression expression, DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, int limit, int offset,
            boolean deepCopy, Set<String> ignored) {
        List<Map<String, Serializable>> maps = new ArrayList<>();
        for (Entry<String, Map<String, Serializable>> en : states.entrySet()) {
            String id = en.getKey();
            if (ignored.contains(id)) {
                continue;
            }
            Map<String, Serializable> map = en.getValue();
            if (evaluator.matches(map)) {
                if (deepCopy) {
                    map = CopyHelper.deepCopy(map);
                }
                maps.add(map);
            }
        }
        // ORDER BY
        if (orderByClause != null) {
            Collections.sort(maps, new OrderByComparator(orderByClause,
                    evaluator));
        }
        // LIMIT / OFFSET
        int totalSize = maps.size();
        if (limit != 0) {
            maps.subList(0, offset > totalSize ? totalSize : offset).clear();
            int size = maps.size();
            if (limit < size) {
                maps.subList(limit, size).clear();
            }
        }
        // TODO DISTINCT

        return new PartialList<>(maps, totalSize);
    }

}
