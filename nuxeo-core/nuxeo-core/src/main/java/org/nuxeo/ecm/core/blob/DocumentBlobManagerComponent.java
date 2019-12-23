/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.Calendar;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.blob.BlobDispatcher.BlobDispatch;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Document.BlobAccessor;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Implementation of the service managing {@link Blob}s associated to a {@link Document} or a repository.
 *
 * @since 9.2
 */
public class DocumentBlobManagerComponent extends DefaultComponent implements DocumentBlobManager {

    private static final Log log = LogFactory.getLog(DocumentBlobManagerComponent.class);

    protected static final String XP = "configuration";

    protected static BlobDispatcher DEFAULT_BLOB_DISPATCHER = new DefaultBlobDispatcher();

    protected static final int BINARY_GC_TX_TIMEOUT_SEC = 86_400; // 1 day

    // in these low-level APIs we deal with unprefixed xpaths, so not file:content
    protected static final String MAIN_BLOB_XPATH = "content";

    protected Deque<BlobDispatcherDescriptor> blobDispatcherDescriptorsRegistry = new LinkedList<>();

    @Override
    public void deactivate(ComponentContext context) {
        blobDispatcherDescriptorsRegistry.clear();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP.equals(extensionPoint)) {
            if (contribution instanceof BlobDispatcherDescriptor) {
                registerBlobDispatcher((BlobDispatcherDescriptor) contribution);
            } else {
                throw new NuxeoException("Invalid descriptor: " + contribution.getClass());
            }
        } else {
            throw new NuxeoException("Invalid extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP.equals(extensionPoint)) {
            if (contribution instanceof BlobDispatcherDescriptor) {
                unregisterBlobDispatcher((BlobDispatcherDescriptor) contribution);
            }
        }
    }

    protected void registerBlobDispatcher(BlobDispatcherDescriptor descr) {
        blobDispatcherDescriptorsRegistry.add(descr);
    }

    protected void unregisterBlobDispatcher(BlobDispatcherDescriptor descr) {
        blobDispatcherDescriptorsRegistry.remove(descr);
    }

    protected BlobDispatcher getBlobDispatcher() {
        BlobDispatcherDescriptor descr = blobDispatcherDescriptorsRegistry.peekLast();
        if (descr == null) {
            return DEFAULT_BLOB_DISPATCHER;
        }
        return descr.getBlobDispatcher();
    }

    protected BlobProvider getBlobProvider(String providerId) {
        return Framework.getService(BlobManager.class).getBlobProvider(providerId);
    }

    protected DocumentBlobProvider getDocumentBlobProvider(Blob blob) {
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob);
        if (blobProvider instanceof DocumentBlobProvider) {
            return (DocumentBlobProvider) blobProvider;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The {@link BlobInfo} (coming from the database) contains the blob key, which may or may not be prefixed by a blob
     * provider id.
     */
    @Override
    public Blob readBlob(BlobInfo blobInfo, String repositoryName) throws IOException {
        String key = blobInfo.key;
        if (key == null) {
            return null;
        }
        BlobProvider blobProvider = getBlobProvider(key, repositoryName);
        if (blobProvider == null) {
            throw new NuxeoException("No registered blob provider for key: " + key);
        }
        return blobProvider.readBlob(blobInfo);
    }

    protected BlobProvider getBlobProvider(String key, String repositoryName) {
        int colon = key.indexOf(':');
        String providerId;
        if (colon < 0) {
            // no prefix, use the blob dispatcher to find the blob provider id
            providerId = getBlobDispatcher().getBlobProvider(repositoryName);
        } else {
            // use the prefix as blob provider id
            providerId = key.substring(0, colon);
        }
        return getBlobProvider(providerId);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the blob is managed and already uses the provider that's expected for this blob and document, there is no need
     * to recompute a key. Otherwise, go through the blob provider.
     */
    @Override
    public String writeBlob(Blob blob, Document doc, String xpath) throws IOException {
        BlobDispatcher blobDispatcher = getBlobDispatcher();
        BlobDispatch dispatch = null;
        if (blob instanceof ManagedBlob) {
            ManagedBlob managedBlob = (ManagedBlob) blob;
            String currentProviderId = managedBlob.getProviderId();
            // is the blob non-transient, so that reusing the key is an option?
            if (!getBlobProvider(currentProviderId).isTransient()) {
                // is it something we don't have to dispatch?
                if (!blobDispatcher.getBlobProviderIds().contains(currentProviderId)) {
                    // not something we have to dispatch, reuse the key
                    return managedBlob.getKey();
                }
                dispatch = blobDispatcher.getBlobProvider(doc, blob, xpath);
                if (dispatch.providerId.equals(currentProviderId)) {
                    // same provider, just reuse the key
                    return managedBlob.getKey();
                }
            }
        }
        if (dispatch == null) {
            dispatch = blobDispatcher.getBlobProvider(doc, blob, xpath);
        }
        BlobProvider blobProvider = getBlobProvider(dispatch.providerId);
        if (blobProvider == null) {
            throw new NuxeoException("No registered blob provider with id: " + dispatch.providerId);
        }
        if (MAIN_BLOB_XPATH.equals(xpath) && blobProvider.isRecordMode() && doc.isUnderRetentionOrLegalHold()) {
            throw new DocumentSecurityException(
                    "Cannot change blob from document " + doc.getUUID() + ", it is under retention / hold");
        }
        String key = blobProvider.writeBlob(new BlobContext(blob, doc, xpath));
        if (dispatch.addPrefix) {
            key = dispatch.providerId + ':' + key;
        }
        return key;
    }

    @Override
    public InputStream getConvertedStream(Blob blob, String mimeType, DocumentModel doc) throws IOException {
        DocumentBlobProvider blobProvider = getDocumentBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getConvertedStream((ManagedBlob) blob, mimeType, doc);
    }

    protected void freezeVersion(BlobAccessor accessor, Document doc) {
        Blob blob = accessor.getBlob();
        DocumentBlobProvider blobProvider = getDocumentBlobProvider(blob);
        if (blobProvider == null) {
            return;
        }
        try {
            Blob newBlob = blobProvider.freezeVersion((ManagedBlob) blob, doc);
            if (newBlob != null) {
                accessor.setBlob(newBlob);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void freezeVersion(Document doc) {
        // finds all blobs, then ask their providers if there's anything to do on check in
        doc.visitBlobs(accessor -> freezeVersion(accessor, doc));
    }

    @Override
    public void notifyChanges(Document doc, Set<String> xpaths) {
        getBlobDispatcher().notifyChanges(doc, xpaths);
    }

    @Override
    public void notifyMakeRecord(Document doc) {
        getBlobDispatcher().notifyMakeRecord(doc);
    }

    @Override
    public void notifyAfterCopy(Document doc) {
        getBlobDispatcher().notifyAfterCopy(doc);
    }

    @Override
    public void notifyBeforeRemove(Document doc) {
        getBlobDispatcher().notifyBeforeRemove(doc);
    }

    @Override
    public void notifySetRetainUntil(Document doc, Calendar retainUntil) {
        updateBlob(doc, context -> context.withUpdateRetainUntil(retainUntil));
    }

    @Override
    public void notifySetLegalHold(Document doc, boolean hold) {
        updateBlob(doc, context -> context.withUpdateLegalHold(hold));
    }

    public void updateBlob(Document doc, Consumer<BlobUpdateContext> contextFiller) {
        ManagedBlob blob = getMainBlob(doc);
        if (blob == null) {
            return;
        }
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blob);
        if (blobProvider == null) {
            log.error("No blob provider found for blob: " + blob.getKey());
            return;
        }
        try {
            String key = stripBlobKeyPrefix(blob.getKey());
            BlobUpdateContext blobUpdateContext = new BlobUpdateContext(key);
            contextFiller.accept(blobUpdateContext);
            blobProvider.updateBlob(blobUpdateContext);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected ManagedBlob getMainBlob(Document doc) {
        Blob blob;
        try {
            blob = (Blob) doc.getValue(MAIN_BLOB_XPATH);
        } catch (PropertyNotFoundException | ClassCastException e) {
            // not a standard file schema
            return null;
        }
        if (blob == null) {
            // no blob in this document
            return null;
        }
        if (blob instanceof ManagedBlob) {
            return (ManagedBlob) blob;
        }
        log.error("Blob is not managed: " + blob);
        return null;
    }

    // TODO restore to version also changes the blob

    protected String stripBlobKeyPrefix(String key) {
        int colon = key.indexOf(':');
        if (colon >= 0) {
            key = key.substring(colon + 1);
        }
        return key;
    }

    // find which GCs to use
    // only GC the binary managers to which we dispatch blobs
    protected List<BinaryGarbageCollector> getGarbageCollectors() {
        List<BinaryGarbageCollector> gcs = new LinkedList<>();
        for (String providerId : getBlobDispatcher().getBlobProviderIds()) {
            BlobProvider blobProvider = getBlobProvider(providerId);
            BinaryGarbageCollector gc = blobProvider.getBinaryGarbageCollector();
            if (gc != null) {
                gcs.add(gc);
            }
        }
        return gcs;
    }

    @Override
    public BinaryManagerStatus garbageCollectBinaries(boolean delete) {
        // do the GC in a long-running transaction to avoid timeouts
        return runInTransaction(() -> {
            List<BinaryGarbageCollector> gcs = getGarbageCollectors();
            // start gc
            long start = System.currentTimeMillis();
            for (BinaryGarbageCollector gc : gcs) {
                gc.start();
            }
            // in all repositories, mark referenced binaries
            // the marking itself will call back into the appropriate gc's mark method
            RepositoryService repositoryService = Framework.getService(RepositoryService.class);
            for (String repositoryName : repositoryService.getRepositoryNames()) {
                Repository repository = repositoryService.getRepository(repositoryName);
                repository.markReferencedBinaries();
            }
            // stop gc
            BinaryManagerStatus globalStatus = new BinaryManagerStatus();
            for (BinaryGarbageCollector gc : gcs) {
                gc.stop(delete);
                BinaryManagerStatus status = gc.getStatus();
                globalStatus.numBinaries += status.numBinaries;
                globalStatus.sizeBinaries += status.sizeBinaries;
                globalStatus.numBinariesGC += status.numBinariesGC;
                globalStatus.sizeBinariesGC += status.sizeBinariesGC;
            }
            globalStatus.gcDuration = System.currentTimeMillis() - start;
            return globalStatus;
        }, BINARY_GC_TX_TIMEOUT_SEC);
    }

    /**
     * Runs the given {@link Supplier} in a transaction with the given {@code timeout}.
     *
     * @since 11.1
     */
    protected <R> R runInTransaction(Supplier<R> supplier, int timeout) {
        if (TransactionHelper.isTransactionMarkedRollback()) {
            throw new NuxeoException("Cannot run supplier when current transaction is marked rollback.");
        }
        boolean txActive = TransactionHelper.isTransactionActive();
        boolean txStarted = false;
        try {
            if (txActive) {
                TransactionHelper.commitOrRollbackTransaction();
            }
            txStarted = TransactionHelper.startTransaction(timeout);
            return supplier.get();
        } finally {
            if (txStarted) {
                TransactionHelper.commitOrRollbackTransaction();
            }
            if (txActive) {
                // go back to default transaction timeout
                TransactionHelper.startTransaction();
            }
        }
    }

    @Override
    public void markReferencedBinary(String key, String repositoryName) {
        BlobProvider blobProvider = getBlobProvider(key, repositoryName);
        BinaryGarbageCollector gc = blobProvider.getBinaryGarbageCollector();
        if (gc != null) {
            key = stripBlobKeyPrefix(key);
            gc.mark(key);
        } else {
            log.error("Unknown binary manager for key: " + key);
        }
    }

    @Override
    public boolean isBinariesGarbageCollectionInProgress() {
        for (BinaryGarbageCollector gc : getGarbageCollectors()) {
            if (gc.isInProgress()) {
                return true;
            }
        }
        return false;
    }

}
