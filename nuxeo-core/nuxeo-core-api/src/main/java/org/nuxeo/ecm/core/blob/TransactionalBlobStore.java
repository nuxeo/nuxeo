/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.blob;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Transactional Blob Store.
 * <p>
 * Until the transaction is committed, blobs are stored in a transient store. Upon commit, they are sent to the
 * permanent store.
 * <p>
 * It is important that a copy operation between the transient store and the permanent store be extremely fast and never fail, as it
 * will be done during commit.
 *
 * @since 11.1
 */
public class TransactionalBlobStore extends AbstractBlobStore implements Synchronization {

    private static final Log log = LogFactory.getLog(TransactionalBlobStore.class);

    public final BlobStore store;

    // may be the same as the permanent store if it has versioning
    public final BlobStore transientStore;

    /**
     * Transient data recording operations applied to a blob, to be executed on the permanent store at commit time.
     */
    public static class TransientInfo {

        /** The key in the transient store of the blob to use, or a delete marker. */
        public String transientKey;

        /** The update to apply. */
        public BlobUpdateContext blobUpdateContext;
    }

    protected final ThreadLocal<Map<String, TransientInfo>> transientInfo = new ThreadLocal<>();

    // the keys that have been modified in any active transaction
    protected final Map<String, Transaction> keysInActiveTransactions = new ConcurrentHashMap<>();

    protected static final String DELETE_MARKER = "";

    protected static boolean isDeleteMarker(String transientKey) {
        return DELETE_MARKER.equals(transientKey);
    }

    public TransactionalBlobStore(BlobStore store, BlobStore transientStore) {
        super("tx", store.getKeyStrategy());
        this.store = store;
        this.transientStore = transientStore;
        if (store.hasVersioning() && transientStore != store) {
            throw new NuxeoException("If the store has versioning then it must be also the transient store");
        }
    }

    @Override
    public BinaryGarbageCollector getBinaryGarbageCollector() {
        return store.getBinaryGarbageCollector();
    }

    @Override
    public boolean hasVersioning() {
        return store.hasVersioning();
    }

    @Override
    public BlobStore unwrap() {
        return store.unwrap();
    }

    @Override
    public String writeBlob(BlobWriteContext blobWriteContext)
            throws IOException {
        if (TransactionHelper.isTransactionActive()) {
            String transientKey;
            String key;
            logTrace("group tx write");
            if (hasVersioning()) {
                // with versioning there can be no collisions, and transientStore = store
                transientKey = transientStore.writeBlob(blobWriteContext);
                key = transientKey;
            } else {
                // for the transient write we use a random key
                transientKey = transientStore.writeBlob(blobWriteContext.copyWithKey(randomString()));
                key = blobWriteContext.getKey(); // may depend on write observer, for example for digests
            }
            try {
                checkConcurrentUpdate(key);
            } catch (ConcurrentUpdateException e) {
                // delete transient store file
                transientStore.deleteBlob(transientKey);
                throw e;
            }
            putTransientKey(key, transientKey);
            logTrace("rnote over Nuxeo: " + key);
            logTrace("end");
            return key;
        } else {
            return store.writeBlob(blobWriteContext);
        }
    }

    @Override
    public boolean copyBlob(String key, BlobStore sourceStore, String sourceKey, boolean atomicMove)
            throws IOException {
        // copyBlob only called from commit or during caching
        throw new UnsupportedOperationException();
    }

    @Override
    public OptionalOrUnknown<Path> getFile(String key) {
        if (TransactionHelper.isTransactionActive()) {
            String transientKey = getTransientKey(key);
            if (isDeleteMarker(transientKey)) {
                return OptionalOrUnknown.missing();
            } else if (transientKey != null) {
                return transientStore.getFile(transientKey);
            }
            // else fall through
        }
        // check permanent store
        return store.getFile(key);
    }

    @Override
    public OptionalOrUnknown<InputStream >getStream(String key) throws IOException {
        if (TransactionHelper.isTransactionActive()) {
            String transientKey = getTransientKey(key);
            if (isDeleteMarker(transientKey)) {
                return OptionalOrUnknown.missing();
            } else if (transientKey != null) {
                OptionalOrUnknown<InputStream> streamOpt = transientStore.getStream(transientKey);
                if (!streamOpt.isPresent()) {
                    log.error("Missing blob from transient blob store: " + transientKey);
                }
                return streamOpt;
            }
            // else fall through
        }
        // check permanent store
        return store.getStream(key);
    }

    @Override
    public boolean readBlob(String key, Path file) throws IOException {
        if (TransactionHelper.isTransactionActive()) {
            logTrace("group tx read");
            logTrace("rnote over Nuxeo: " + key);
            String transientKey = getTransientKey(key);
            boolean found;
            if (isDeleteMarker(transientKey)) {
                logTrace("<--", "deleted");
                found = false; // deleted in transaction
            } else if (transientKey != null) {
                found = transientStore.readBlob(transientKey, file);
                if (!found) {
                    log.error("Missing blob from transient blob store: " + transientKey);
                }
            } else {
                // else check permanent store
                found = store.readBlob(key, file);
            }
            logTrace("end");
            return found;
        }
        // check permanent store
        return store.readBlob(key, file);
    }

    @Override
    public void writeBlobProperties(BlobUpdateContext blobUpdateContext) throws IOException {
        if (TransactionHelper.isTransactionActive()) {
            String key = blobUpdateContext.key;
            checkConcurrentUpdate(key);
            putTransientUpdate(key, blobUpdateContext);
        } else {
            store.writeBlobProperties(blobUpdateContext);
        }
    }

    @Override
    public void deleteBlob(String key) {
        if (TransactionHelper.isTransactionActive()) {
            checkConcurrentUpdate(key);
            putTransientKey(key, DELETE_MARKER);
        } else {
            store.deleteBlob(key);
        }
    }

    // called when we know a transaction is already active
    // checks for concurrent update before writing to key
    protected void checkConcurrentUpdate(String key) {
        Transaction tx = getTransaction();
        Transaction otherTx = keysInActiveTransactions.putIfAbsent(key, tx);
        if (otherTx != null) {
            if (otherTx != tx) {
                throw new ConcurrentUpdateException(key);
            }
            // there may be a previous transient file
            // it's now unneeded as we're about to overwrite it
            String otherTransientKey = getTransientKey(key);
            if (otherTransientKey != null && !isDeleteMarker(otherTransientKey)) {
                transientStore.deleteBlob(otherTransientKey);
            }
        }
    }

    protected Transaction getTransaction() {
        try {
            return NuxeoContainer.getTransactionManager().getTransaction();
        } catch (NullPointerException | SystemException e) {
            throw new NuxeoException(e);
        }
    }

    // ---------- Synchronization ----------

    protected String getTransientKey(String key) {
        Map<String, TransientInfo> map = transientInfo.get();
        if (map == null) {
            return null;
        } else {
            TransientInfo info = map.get(key);
            return info == null ? null : info.transientKey;
        }
    }

    protected void putTransientKey(String key, String transientKey) {
        TransientInfo info = getTransientInfo(key);
        info.transientKey = transientKey;
    }

    protected void putTransientUpdate(String key, BlobUpdateContext blobUpdateContext) {
        TransientInfo info = getTransientInfo(key);
        if (info.blobUpdateContext == null) {
            info.blobUpdateContext = blobUpdateContext;
        } else {
            info.blobUpdateContext.with(blobUpdateContext);
        }
    }

    protected TransientInfo getTransientInfo(String key) {
        Map<String, TransientInfo> map = transientInfo.get();
        if (map == null) {
            map = new HashMap<>();
            transientInfo.set(map);
            TransactionHelper.registerSynchronization(this);
        }
        return map.computeIfAbsent(key, k -> new TransientInfo());
    }

    @Override
    public void beforeCompletion() {
        // nothing to do
    }

    @Override
    public void afterCompletion(int status) {
        Map<String, TransientInfo> map = transientInfo.get();
        transientInfo.remove();
        try {
            if (status == Status.STATUS_COMMITTED) {
                logTrace("== TX commit ==");
                // move transient files to permanent store
                for (Entry<String, TransientInfo> en : map.entrySet()) {
                    String key = en.getKey();
                    TransientInfo info = en.getValue();
                    String transientKey = info.transientKey;
                    boolean applyUpdates = false;
                    if (transientKey != null) {
                        if (isDeleteMarker(transientKey)) {
                            store.deleteBlob(key);
                        } else {
                            if (hasVersioning()) {
                                // with versioning, the blob already has its final key
                                applyUpdates = true;
                            } else {
                                // without versioning, atomically move to permanent store
                                try {
                                    boolean found = store.copyBlob(key, transientStore, transientKey, true);
                                    if (found) {
                                        applyUpdates = true;
                                    } else {
                                        log.error("Missing blob from transient blob store: " + transientKey
                                                + ", failed to commit creation of file: " + key);
                                    }
                                } catch (IOException e) {
                                    log.error("Failed to commit creation of blob: " + key, e);
                                }
                            }
                        }
                    }
                    // apply updates
                    if (applyUpdates) {
                        BlobUpdateContext blobUpdateContext = info.blobUpdateContext;
                        if (blobUpdateContext != null) {
                            try {
                                store.writeBlobProperties(blobUpdateContext);
                            } catch (IOException e) {
                                log.error("Failed to commit update of blob: " + key, e);
                            }
                        }
                    }
                }
            } else if (status == Status.STATUS_ROLLEDBACK) {
                logTrace("== TX rollback ==");
                // delete transient files
                for (TransientInfo info : map.values()) {
                    String transientKey = info.transientKey;
                    if (transientKey != null && !isDeleteMarker(transientKey)) {
                        transientStore.deleteBlob(transientKey);
                    }
                }
            } else {
                log.error("Unexpected afterCompletion status: " + status);
            }
        } finally {
            logTrace("== TX end ==");
            keysInActiveTransactions.keySet().removeAll(map.keySet());
        }
    }

}
