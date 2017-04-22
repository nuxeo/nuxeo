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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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

    protected static final String XP = "configuration";

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
    public Map<String, BlobProvider> getBlobProviders() {
        return blobProviders;
    }

}
