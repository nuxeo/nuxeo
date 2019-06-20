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
package org.nuxeo.ecm.core.blob.binary;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.AbstractBlobProvider;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.trackers.files.FileEventTracker;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Blob Provider storing blobs on a local filesystem like {@link DefaultBinaryManager} but in "record mode".
 * <p>
 * See {@link BlobProvider#isRecordMode} for a description of the capabilities and constraints of the record mode.
 * <p>
 * This blob provider is transactional: when in a transaction, blobs are first stored in a transient space (tmpDir), and
 * only moved to their final location at commit time.
 *
 * @since 11.1
 */
public class DefaultRecordBlobProvider extends AbstractBlobProvider implements Synchronization {

    private static final Log log = LogFactory.getLog(DefaultRecordBlobProvider.class);

    // in these low-level APIs we deal with unprefixed xpaths, so not file:content
    protected static final String MAIN_BLOB_XPATH = "content";

    // the files that have been created in the transaction
    // a map of key to 1. temporary key of the blob, or 2. empty string for a delete
    protected final ThreadLocal<Map<String, String>> transientKeys = new ThreadLocal<>();

    // the keys that have been modified in any active transaction
    protected final Map<String, Transaction> keysInActiveTransactions = new ConcurrentHashMap<>();

    protected File storageDir;

    // used as a temporary space for transactions
    protected File tmpDir;

    @Override
    public boolean isRecordMode() {
        return true;
    }

    protected static final String DELETE_MARKER = "";

    protected static boolean isDeleteMarker(@NotNull String key) {
        return key.isEmpty();
    }

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        File base = LocalBinaryManager.getStorageBase(properties);
        log.info("Registering record blob provider '" + blobProviderId + "' at path: " + base);
        storageDir = new File(base, LocalBinaryManager.DATA);
        tmpDir = new File(base, LocalBinaryManager.TMP);
        storageDir.mkdirs();
        tmpDir.mkdirs();
        FileEventTracker.registerProtectedPath(storageDir.getAbsolutePath());
    }

    @Override
    public void close() {
        if (tmpDir != null) {
            try {
                FileUtils.cleanDirectory(tmpDir);
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }
    }

    // for tests
    public File getStorageDir() {
        return storageDir;
    }

    // for tests
    public File getTmpDir() {
        return tmpDir;
    }

    @Override
    public String writeBlob(Blob blob, String id, String xpath) throws IOException {
        if (!MAIN_BLOB_XPATH.equals(xpath)) {
            throw new NuxeoException(
                    "Cannot store blob at xpath '" + xpath + "' in record blob provider: " + blobProviderId);
        }
        if (StringUtils.isEmpty(id)) {
            throw new NuxeoException("Missing id");
        }
        String key = getKey(id);
        if (TransactionHelper.isTransactionActive()) {
            checkConcurrentUpdate(key, id);
            String transientKey = key + "." + ThreadLocalRandom.current().nextLong();
            // store the transient key
            getTransactionKeyMap().put(key, transientKey);
            // store in transient space
            // do this last, so that if there's a rollback we can remove it
            writeBlobImmediate(blob, transientKey, true);
            return key;
        } else {
            // no transaction, direct store
            writeBlobImmediate(blob, key, false);
            return key;
        }
    }

    @Override
    public String writeBlob(Blob blob) throws IOException {
        throw new UnsupportedOperationException("Must use writeBlob(Blob, String, String)");
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        String key = blobInfo.key;
        // strip prefix
        int colon = key.indexOf(':');
        if (colon >= 0) {
            key = key.substring(colon + 1);
        }
        Map<String, String> map = transientKeys.get();
        if (map != null) {
            // check in transient space first
            String transientKey = map.get(key);
            if (transientKey == null) {
                // not present in transient space, continue
            } else if (isDeleteMarker(transientKey)) {
                throw new IOException("Nonexistent file for key: " + key);
            } else {
                Blob blob = readBlobImmediate(blobInfo, transientKey, true);
                if (blob == null) {
                    // corrupted storage
                    throw new IOException("Missing expected file for key: " + transientKey);
                }
                return blob;
            }
        } else {
            transientKeys.remove(); // remove spurious null from thread-local
        }
        // then check actual storage
        Blob blob = readBlobImmediate(blobInfo, key, false);
        if (blob == null) {
            throw new IOException("Nonexistent file for key: " + key);
        }
        return blob;
    }

    @Override
    public void deleteBlob(String id, String xpath) {
        if (!MAIN_BLOB_XPATH.equals(xpath)) {
            throw new NuxeoException(
                    "Cannot store blob at xpath '" + xpath + "' in record blob provider: " + blobProviderId);
        }
        if (StringUtils.isEmpty(id)) {
            throw new NuxeoException("Missing id");
        }
        String key = getKey(id);
        if (TransactionHelper.isTransactionActive()) {
            checkConcurrentUpdate(key, id);
            // store a delete marker
            getTransactionKeyMap().put(key, DELETE_MARKER);
        } else {
            // no transaction, direct delete
            File file = getFile(key, false);
            FileUtils.deleteQuietly(file);
        }
    }

    // called when we know a transaction is already active
    protected void checkConcurrentUpdate(String key, String id) {
        Transaction tx = getTransaction();
        Transaction otherTx = keysInActiveTransactions.putIfAbsent(key, tx);
        if (otherTx != null) {
            if (otherTx != tx) {
                throw new ConcurrentUpdateException(id);
            }
            // there may be a previous transient file
            // it's now unneeded as we're about to overwrite it
            String otherTransientKey = getTransactionKeyMap().get(key);
            if (otherTransientKey != null) {
                deleteTransient(otherTransientKey);
            }
        }
    }

    protected Transaction getTransaction() {
        try {
            return NuxeoContainer.getTransactionManager().getTransaction();
        } catch (NullPointerException | SystemException e) {
            throw new NuxeoException();
        }
    }

    protected String getKey(String id) {
        return id;
    }

    protected File getFile(String key, boolean isTransient) {
        return new File(isTransient ? tmpDir : storageDir, key);
    }

    protected void writeBlobImmediate(Blob blob, String key, boolean isTransient) throws IOException {
        File tmp = File.createTempFile("create_", ".tmp", tmpDir);
        try {
            // write the blob to a temporary file
            blob.transferTo(tmp);
            // move the temporary file to its destination
            File file = getFile(key, isTransient);
            atomicMoveOrReplace(tmp, file);
        } finally {
            tmp.delete();
        }
    }

    protected Blob readBlobImmediate(BlobInfo blobInfo, String key, boolean isTransient) throws IOException {
        // strip prefix TODO this should be done already
        int colon = key.indexOf(':');
        if (colon >= 0) {
            key = key.substring(colon + 1);
        }
        File file = getFile(key, isTransient);
        if (!file.exists()) {
            return null;
        }
        Binary binary = new Binary(file, key, blobProviderId);
        long length;
        if (blobInfo.length == null) {
            log.debug("Missing blob length for: " + blobInfo.key);
            // to avoid crashing, get the length from the binary's file
            length = file.length();
        } else {
            length = blobInfo.length.longValue();
        }
        return new BinaryBlob(binary, blobInfo.key, blobInfo.filename, blobInfo.mimeType, blobInfo.encoding,
                blobInfo.digest, length);
    }

    protected void deleteTransient(String key) {
        if (!isDeleteMarker(key)) {
            File file = getFile(key, true);
            FileUtils.deleteQuietly(file);
        }
    }

    // ---------- Synchronization ----------

    protected Map<String, String> getTransactionKeyMap() {
        Map<String, String> transactionKeyMap = transientKeys.get();
        if (transactionKeyMap == null) {
            transactionKeyMap = new HashMap<>();
            transientKeys.set(transactionKeyMap);
            TransactionHelper.registerSynchronization(this);
        }
        return transactionKeyMap;
    }

    @Override
    public void beforeCompletion() {
        // nothing to do
    }

    @Override
    public void afterCompletion(int status) {
        Map<String, String> transactionKeyMap = transientKeys.get();
        transientKeys.remove();
        try {
            if (status == Status.STATUS_COMMITTED) {
                // move transient files to permanent storage
                for (Entry<String, String> en : transactionKeyMap.entrySet()) {
                    String key = en.getKey();
                    String transientKey = en.getValue();
                    File file = getFile(key, false);
                    if (isDeleteMarker(transientKey)) {
                        FileUtils.deleteQuietly(file);
                    } else {
                        File transientFile = getFile(transientKey, true);
                        if (!transientFile.exists()) {
                            // corrupted storage
                            log.error("Missing expected file for key: " + transientKey);
                        }
                        try {
                            atomicMoveOrReplace(transientFile, file);
                        } catch (IOException e) {
                            log.error("Failed to move file", e);
                        }
                    }
                }
            } else if (status == Status.STATUS_ROLLEDBACK) {
                // delete transient files
                for (String transientKey : transactionKeyMap.values()) {
                    deleteTransient(transientKey);
                }
            } else {
                log.error("Unexpected afterCompletion status: " + status);
            }
        } finally {
            keysInActiveTransactions.keySet().removeAll(transactionKeyMap.keySet());
        }
    }

    public static void atomicMoveOrReplace(File src, File dest) throws IOException {
        Path path = src.toPath();
        Path destPath = dest.toPath();
        try {
            Files.move(path, destPath, ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // shouldn't happen, given our choice of tmp and storage locations
            // do a copy through a tmp file on the same filesystem then atomic rename
            Path tmp = Files.createTempFile(destPath.getParent(), null, null);
            try {
                Files.copy(path, tmp, REPLACE_EXISTING);
                Files.move(tmp, destPath, ATOMIC_MOVE);
                Files.delete(path);
            } catch (IOException ioe) {
                // don't leave tmp file in case of error
                Files.deleteIfExists(tmp);
                throw ioe;
            }
        }
    }

}
