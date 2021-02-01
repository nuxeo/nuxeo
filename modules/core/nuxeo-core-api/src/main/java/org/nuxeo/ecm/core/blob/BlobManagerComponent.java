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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.binary.BinaryBlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation of the service managing the storage and retrieval of {@link Blob}s, through internally-registered
 * {@link BlobProvider}s.
 *
 * @since 7.2
 */
public class BlobManagerComponent extends DefaultComponent implements BlobManager {

    protected static final String XP = "configuration";

    public static final String DEFAULT_ID = "default";

    /**
     * Blob providers whose id starts with this prefix are automatically marked transient.
     *
     * @since 10.10
     * @see BlobProvider#isTransient
     */
    public static final String TRANSIENT_ID_PREFIX = "transient";

    /** @since 11.5 */
    public static final String BLOB_KEY_REPLACEMENT_KV = "blobKeyReplacement";

    protected static final Duration BLOB_KEY_REPLACEMENT_TTL = Duration.ofHours(1);

    protected boolean blobProvidersInitialized;

    protected Map<String, BlobProvider> blobProviders = new ConcurrentHashMap<>();

    @Override
    public void start(ComponentContext context) {
        synchronized (this) {
            blobProvidersInitialized = false;
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        // close each blob provider
        for (BlobProvider blobProvider : blobProviders.values()) {
            blobProvider.close();
        }
        blobProviders.clear();
        synchronized (this) {
            blobProvidersInitialized = false;
        }
    }

    // public for tests
    public void registerBlobProvider(BlobProviderDescriptor descr) {
        closeOldBlobProvider(descr.name);
        blobProviders.put(descr.name, createBlobProvider(descr));
    }

    // public for tests
    public void unregisterBlobProvider(BlobProviderDescriptor descr) {
        closeOldBlobProvider(descr.name);
        blobProviders.remove(descr.name);
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

    /**
     * Delayed blob provider creation: the blob provider initialization may require a connection that has not been
     * registered yet.
     */
    protected BlobProvider createBlobProvider(BlobProviderDescriptor descr) {
        BlobProvider blobProvider;
        String providerId = descr.name;
        Class<?> klass = descr.klass;
        Map<String, String> properties = descr.properties;
        try {
            if (BlobProvider.class.isAssignableFrom(klass)) {
                @SuppressWarnings("unchecked")
                Class<? extends BlobProvider> blobProviderClass = (Class<? extends BlobProvider>) klass;
                blobProvider = blobProviderClass.getDeclaredConstructor().newInstance();
            } else if (BinaryManager.class.isAssignableFrom(klass)) {
                @SuppressWarnings("unchecked")
                Class<? extends BinaryManager> binaryManagerClass = (Class<? extends BinaryManager>) klass;
                BinaryManager binaryManager = binaryManagerClass.getDeclaredConstructor().newInstance();
                blobProvider = new BinaryBlobProvider(binaryManager);
            } else {
                throw new RuntimeException("Unknown class for blob provider: " + klass);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        // make it transient if needed
        if (providerId.startsWith(TRANSIENT_ID_PREFIX)) {
            descr.properties.put(BlobProviderDescriptor.TRANSIENT, "true");
        }
        try {
            blobProvider.initialize(providerId, properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return blobProvider;
    }

    @Override
    public synchronized BlobProvider getBlobProvider(String providerId) {
        BlobProvider blobProvider = blobProviders.get(providerId);
        if (blobProvider == null) {
            this.<BlobProviderDescriptor> getRegistryContribution(XP, providerId)
                .ifPresent(descr -> blobProviders.put(providerId, createBlobProvider(descr)));
            return blobProviders.get(providerId);
        }
        return blobProvider;
    }

    @Override
    public synchronized BlobProvider getBlobProviderWithNamespace(String providerId, String defaultId) {
        BlobProvider blobProvider = getBlobProvider(providerId);
        if (blobProvider != null) {
            return blobProvider;
        }
        // create and register a blob provider from the "default" configuration
        BlobProviderDescriptor defaultDescr = this.<BlobProviderDescriptor> getRegistryContribution(XP, defaultId)
                                                  .orElseThrow(() -> new NuxeoException(
                                                          "Missing configuration for blob provider: " + defaultId));
        // copy
        BlobProviderDescriptor descr = new BlobProviderDescriptor(defaultDescr);
        // set new name and namespace
        descr.name = providerId;
        descr.properties.put(BlobProviderDescriptor.NAMESPACE, providerId);
        // register and return it
        registerBlobProvider(descr);
        return getBlobProvider(providerId);
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
    @Deprecated
    public InputStream getStream(Blob blob) throws IOException {
        return blob instanceof ManagedBlob ? blob.getStream() : null;
    }

    @Override
    @Deprecated
    public File getFile(Blob blob) {
        return blob instanceof ManagedBlob ? blob.getFile() : null;
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
    public synchronized Map<String, BlobProvider> getBlobProviders() {
        if (!blobProvidersInitialized) {
            this.<BlobProviderDescriptor> getRegistryContributions(XP)
                .stream()
                .filter(desc -> !blobProviders.containsKey(desc.name))
                .forEach(this::registerBlobProvider);
            blobProvidersInitialized = true;
        }
        return Collections.unmodifiableMap(blobProviders);
    }

    protected KeyValueStore getBlobKeyReplacementKeyValuestore() {
        KeyValueService kvService = Framework.getService(KeyValueService.class);
        return kvService == null ? null : kvService.getKeyValueStore(BLOB_KEY_REPLACEMENT_KV);
    }

    @Override
    public void setBlobKeyReplacement(String blobProviderId, String key, String newKey) {
        KeyValueStore kvStore = getBlobKeyReplacementKeyValuestore();
        if (kvStore == null) {
            return;
        }
        kvStore.put(blobProviderId + ':' + key, newKey, BLOB_KEY_REPLACEMENT_TTL.getSeconds());
    }

    @Override
    public String getBlobKeyReplacement(String blobProviderId, String key) {
        KeyValueStore kvStore = getBlobKeyReplacementKeyValuestore();
        if (kvStore == null) {
            return key;
        }
        String newKey = kvStore.getString(blobProviderId + ':' + key);
        return newKey == null ? key : newKey;
    }

}
