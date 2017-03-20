/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.net.URI;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobDispatcher.BlobDispatch;
import org.nuxeo.ecm.core.blob.binary.BinaryBlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Document.BlobAccessor;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Implementation of the service managing the storage and retrieval of {@link Blob}s, through internally-registered
 * {@link BlobProvider}s.
 *
 * @since 7.2
 */
public class BlobManagerComponent extends DefaultComponent implements BlobManager {

    private static final Log log = LogFactory.getLog(BlobManagerComponent.class);

    protected static final String XP = "configuration";

    protected static BlobDispatcher DEFAULT_BLOB_DISPATCHER = new DefaultBlobDispatcher();

    protected Deque<BlobDispatcherDescriptor> blobDispatcherDescriptorsRegistry = new LinkedList<>();

    protected BlobProviderDescriptorRegistry blobProviderDescriptorsRegistry = new BlobProviderDescriptorRegistry();

    protected Map<String, BlobProvider> blobProviders = new HashMap<>();

    protected static class BlobProviderDescriptorRegistry extends SimpleContributionRegistry<BlobProviderDescriptor> {

        @Override
        public String getContributionId(BlobProviderDescriptor contrib) {
            return contrib.name;
        }

        @Override
        public BlobProviderDescriptor clone(BlobProviderDescriptor orig) {
            return new BlobProviderDescriptor(orig);
        }

        @Override
        public void merge(BlobProviderDescriptor src, BlobProviderDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        public void clear() {
            currentContribs.clear();
        }

        public BlobProviderDescriptor getBlobProviderDescriptor(String id) {
            return getCurrentContribution(id);
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
        blobDispatcherDescriptorsRegistry.clear();
        blobProviderDescriptorsRegistry.clear();
        // close each blob provider
        for (BlobProvider blobProvider : blobProviders.values()) {
            blobProvider.close();
        }
        blobProviders.clear();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP.equals(extensionPoint)) {
            if (contribution instanceof BlobDispatcherDescriptor) {
                registerBlobDispatcher((BlobDispatcherDescriptor) contribution);
            } else if (contribution instanceof BlobProviderDescriptor) {
                registerBlobProvider((BlobProviderDescriptor) contribution);
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
            } else if (contribution instanceof BlobProviderDescriptor) {
                unregisterBlobProvider((BlobProviderDescriptor) contribution);
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

    // public for tests
    public void registerBlobProvider(BlobProviderDescriptor descr) {
        closeOldBlobProvider(descr.name);
        blobProviderDescriptorsRegistry.addContribution(descr);
        // lookup now to have immediate feedback on eror
        getBlobProvider(descr.name);
    }

    // public for tests
    public void unregisterBlobProvider(BlobProviderDescriptor descr) {
        closeOldBlobProvider(descr.name);
        blobProviderDescriptorsRegistry.removeContribution(descr);
    }

    /**
     * We're about to change something about a contributed blob provider. Close the old one.
     */
    protected synchronized void closeOldBlobProvider(String id) {
        BlobProvider blobProvider = blobProviders.remove(id);
        if (blobProvider != null) {
            blobProvider.close();
        }
    }

    @Override
    public synchronized BlobProvider getBlobProvider(String providerId) {
        BlobProvider blobProvider = blobProviders.get(providerId);
        if (blobProvider == null) {
            BlobProviderDescriptor descr = blobProviderDescriptorsRegistry.getBlobProviderDescriptor(providerId);
            if (descr == null) {
                return null;
            }
            Class<?> klass = descr.klass;
            Map<String, String> properties = descr.properties;
            try {
                if (BlobProvider.class.isAssignableFrom(klass)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends BlobProvider> blobProviderClass = (Class<? extends BlobProvider>) klass;
                    blobProvider = blobProviderClass.newInstance();
                } else if (BinaryManager.class.isAssignableFrom(klass)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends BinaryManager> binaryManagerClass = (Class<? extends BinaryManager>) klass;
                    BinaryManager binaryManager = binaryManagerClass.newInstance();
                    blobProvider = new BinaryBlobProvider(binaryManager);
                } else {
                    throw new RuntimeException("Unknown class for blob provider: " + klass);
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            try {
                blobProvider.initialize(providerId, properties);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            blobProviders.put(providerId, blobProvider);
        }
        return blobProvider;
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
        if (dispatch == null) {
            dispatch = blobDispatcher.getBlobProvider(doc, blob, xpath);
        }
        BlobProvider blobProvider = getBlobProvider(dispatch.providerId);
        if (blobProvider == null) {
            throw new NuxeoException("No registered blob provider with id: " + dispatch.providerId);
        }
        String key = blobProvider.writeBlob(blob, doc);
        if (dispatch.addPrefix) {
            key = dispatch.providerId + ':' + key;
        }
        return key;
    }

    @Override
    public BlobProvider getBlobProvider(Blob blob) {
        if (!(blob instanceof ManagedBlob)) {
            return null;
        }
        ManagedBlob managedBlob = (ManagedBlob) blob;
        return getBlobProvider(managedBlob.getProviderId());
    }

    @Override
    public InputStream getStream(Blob blob) throws IOException {
        BlobProvider blobProvider = getBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getStream((ManagedBlob) blob);
    }

    @Override
    public InputStream getThumbnail(Blob blob) throws IOException {
        BlobProvider blobProvider = getBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getThumbnail((ManagedBlob) blob);
    }

    @Override
    public URI getURI(Blob blob, UsageHint hint, HttpServletRequest servletRequest) throws IOException {
        BlobProvider blobProvider = getBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getURI((ManagedBlob) blob, hint, servletRequest);
    }

    @Override
    public Map<String, URI> getAvailableConversions(Blob blob, UsageHint hint) throws IOException {
        BlobProvider blobProvider = getBlobProvider(blob);
        if (blobProvider == null) {
            return Collections.emptyMap();
        }
        return blobProvider.getAvailableConversions((ManagedBlob) blob, hint);
    }

    @Override
    public InputStream getConvertedStream(Blob blob, String mimeType, DocumentModel doc) throws IOException {
        BlobProvider blobProvider = getBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getConvertedStream((ManagedBlob) blob, mimeType, doc);
    }

    protected void freezeVersion(BlobAccessor accessor, Document doc) {
        Blob blob = accessor.getBlob();
        BlobProvider blobProvider = getBlobProvider(blob);
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
    public Map<String, BlobProvider> getBlobProviders() {
        return blobProviders;
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

    // find which GCs to use
    // only GC the binary managers to which we dispatch blobs
    protected List<BinaryGarbageCollector> getGarbageCollectors() {
        List<BinaryGarbageCollector> gcs = new LinkedList<>();
        for (String providerId : getBlobDispatcher().getBlobProviderIds()) {
            BlobProvider blobProvider = getBlobProvider(providerId);
            BinaryManager binaryManager = blobProvider.getBinaryManager();
            if (binaryManager != null) {
                gcs.add(binaryManager.getGarbageCollector());
            }
        }
        return gcs;
    }

    @Override
    public BinaryManagerStatus garbageCollectBinaries(boolean delete) {
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
    }

    @Override
    public void markReferencedBinary(String key, String repositoryName) {
        BlobProvider blobProvider = getBlobProvider(key, repositoryName);
        BinaryManager binaryManager = blobProvider.getBinaryManager();
        if (binaryManager != null) {
            int colon = key.indexOf(':');
            if (colon > 0) {
                // if the key is in the "providerId:digest" format, keep only the real digest
                key = key.substring(colon + 1);
            }
            binaryManager.getGarbageCollector().mark(key);
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
