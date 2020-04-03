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
package org.nuxeo.ecm.core.storage.mem;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_BLOB_DATA;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_FULLTEXT_BINARY;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_CREATED;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_LOCK_OWNER;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.resource.spi.ConnectionManager;

import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.dbs.DBSDocument;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryBase;
import org.nuxeo.runtime.api.Framework;

/**
 * In-memory implementation of a {@link Repository}.
 * <p>
 * Internally, the repository is a map from id to document object.
 * <p>
 * A document object is a JSON-like document stored as a Map recursively containing the data, see {@link DBSDocument}
 * for the description of the document.
 *
 * @since 5.9.4
 */
public class MemRepository extends DBSRepositoryBase {

    protected static final String NOSCROLL_ID = "noscroll";

    // for debug
    private final AtomicLong temporaryIdCounter = new AtomicLong(0);

    /**
     * The content of the repository, a map of document id -> object.
     */
    protected Map<String, State> states;

    public MemRepository(ConnectionManager cm, MemRepositoryDescriptor descriptor) {
        super(cm, descriptor.name, descriptor);
        initRepository();
    }

    @Override
    public List<IdType> getAllowedIdTypes() {
        return Collections.singletonList(IdType.varchar);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        states = null;
    }

    protected void initRepository() {
        states = new ConcurrentHashMap<>();
        try (MemConnection connection = getConnection()) {
            connection.initRepository();
        }
    }

    protected String generateNewId() {
        if (DBSRepositoryBase.DEBUG_UUIDS) {
            return "UUID_" + temporaryIdCounter.incrementAndGet();
        } else {
            return UUID.randomUUID().toString();
        }
    }

    @Override
    public boolean supportsTransactions() {
        return false;
    }

    @Override
    public MemConnection getConnection() {
        return new MemConnection(this);
    }

    /* synchronized */
    @Override
    public synchronized Lock getLock(String id) {
        State state = states.get(id);
        if (state == null) {
            // document not found
            throw new DocumentNotFoundException(id);
        }
        String owner = (String) state.get(KEY_LOCK_OWNER);
        if (owner == null) {
            return null;
        }
        Calendar created = (Calendar) state.get(KEY_LOCK_CREATED);
        return new Lock(owner, created);
    }

    /* synchronized */
    @Override
    public synchronized Lock setLock(String id, Lock lock) {
        State state = states.get(id);
        if (state == null) {
            // document not found
            throw new DocumentNotFoundException(id);
        }
        String owner = (String) state.get(KEY_LOCK_OWNER);
        if (owner != null) {
            // return old lock
            Calendar created = (Calendar) state.get(KEY_LOCK_CREATED);
            return new Lock(owner, created);
        }
        state.put(KEY_LOCK_OWNER, lock.getOwner());
        state.put(KEY_LOCK_CREATED, lock.getCreated());
        return null;
    }

    /* synchronized */
    @Override
    public synchronized Lock removeLock(String id, String owner) {
        State state = states.get(id);
        if (state == null) {
            // document not found
            throw new DocumentNotFoundException(id);
        }
        String oldOwner = (String) state.get(KEY_LOCK_OWNER);
        if (oldOwner == null) {
            // no previous lock
            return null;
        }
        Calendar oldCreated = (Calendar) state.get(KEY_LOCK_CREATED);
        if (!LockManager.canLockBeRemoved(oldOwner, owner)) {
            // existing mismatched lock, flag failure
            return new Lock(oldOwner, oldCreated, true);
        }
        // remove lock
        state.put(KEY_LOCK_OWNER, null);
        state.put(KEY_LOCK_CREATED, null);
        // return old lock
        return new Lock(oldOwner, oldCreated);
    }

    protected List<List<String>> binaryPaths;

    @Override
    protected void initBlobsPaths() {
        MemBlobFinder finder = new MemBlobFinder();
        finder.visit();
        binaryPaths = finder.binaryPaths;
    }

    protected static class MemBlobFinder extends BlobFinder {
        protected List<List<String>> binaryPaths = new ArrayList<>();

        @Override
        protected void recordBlobPath() {
            binaryPaths.add(new ArrayList<>(path));
        }
    }

    @Override
    public void markReferencedBinaries() {
        DocumentBlobManager blobManager = Framework.getService(DocumentBlobManager.class);
        for (State state : states.values()) {
            for (List<String> path : binaryPaths) {
                markReferencedBinaries(state, path, 0, blobManager);
            }
        }
    }

    protected void markReferencedBinaries(State state, List<String> path, int start, DocumentBlobManager blobManager) {
        for (int i = start; i < path.size(); i++) {
            String name = path.get(i);
            Serializable value = state.get(name);
            if (value instanceof State) {
                state = (State) value;
            } else {
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) value;
                    for (Object v : list) {
                        if (v instanceof State) {
                            markReferencedBinaries((State) v, path, i + 1, blobManager);
                        } else {
                            markReferencedBinary(v, blobManager);
                        }
                    }
                }
                state = null;
                break;
            }
        }
        if (state != null) {
            Serializable data = state.get(KEY_BLOB_DATA);
            markReferencedBinary(data, blobManager);
            if (isFulltextStoredInBlob()) {
                data = state.get(KEY_FULLTEXT_BINARY);
                markReferencedBinary(data, blobManager);
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

}
