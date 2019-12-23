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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.binary.BinaryBlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
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

    public static final String DEFAULT_ID = "default";

    /**
     * Blob providers whose id starts with this prefix are automatically marked transient.
     *
     * @since 10.10
     * @see BlobProvider#isTransient
     */
    public static final String TRANSIENT_ID_PREFIX = "transient";

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

        public Set<String> getBlobProviderIds() {
            return currentContribs.keySet();
        }
    }

    @Override
    public void deactivate(ComponentContext context) {
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
            if (contribution instanceof BlobProviderDescriptor) {
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
            if (contribution instanceof BlobProviderDescriptor) {
                unregisterBlobProvider((BlobProviderDescriptor) contribution);
            }
        }
    }

    // public for tests
    public void registerBlobProvider(BlobProviderDescriptor descr) {
        closeOldBlobProvider(descr.name);
        blobProviderDescriptorsRegistry.addContribution(descr);
        // We can't look up the blob provider right now to initialize it, the platform has not started yet
        // and the blob provider initialization may require a connection that has not been registered yet.
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
            // make it transient if needed
            if (providerId.startsWith(TRANSIENT_ID_PREFIX)) {
                descr.properties.put(BlobProviderDescriptor.TRANSIENT, "true");
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

    @Override
    public synchronized BlobProvider getBlobProviderWithNamespace(String providerId, String defaultId) {
        BlobProvider blobProvider = getBlobProvider(providerId);
        if (blobProvider != null) {
            return blobProvider;
        }
        // create and register a blob provider from the "default" configuration
        BlobProviderDescriptor defaultDescr = blobProviderDescriptorsRegistry.getBlobProviderDescriptor(defaultId);
        if (defaultDescr == null) {
            throw new NuxeoException("Missing configuration for blob provider: " + defaultId);
        }
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
    public InputStream getStream(Blob blob) throws IOException {
        BlobProvider blobProvider = getBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        try {
            return blobProvider.getStream((ManagedBlob) blob);
        } catch (IOException e) {
            // we don't want to crash everything if the remote file cannot be accessed
            log.debug(e, e);
            log.error("Failed to access file: " + ((ManagedBlob) blob).getKey());
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    @Override
    public File getFile(Blob blob) {
        BlobProvider blobProvider = getBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getFile((ManagedBlob) blob);
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
        Set<String> blobProviderIds = blobProviderDescriptorsRegistry.getBlobProviderIds();
        if (blobProviders.size() != blobProviderIds.size()) {
            // register all providers
            for (String id : blobProviderIds) {
                getBlobProvider(id); // instantiate and initialize
            }
        }
        return blobProviders;
    }

}
