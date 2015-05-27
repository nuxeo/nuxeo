/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.core.blob.BlobDispatcher.BlobDispatch;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.binary.BinaryBlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Document.BlobAccessor;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.TypeProvider;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
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
        int colon = key.indexOf(':');
        String providerId;
        if (colon < 0) {
            // no prefix, use the blob dispatcher to find the blob provider id
            providerId = getBlobDispatcher().getBlobProvider(repositoryName);
        } else {
            // use the prefix as blob provider id
            providerId = key.substring(0, colon);
        }
        BlobProvider blobProvider = getBlobProvider(providerId);
        if (blobProvider == null) {
            throw new NuxeoException(
                    "No registered blob provider with id: " + providerId + " for key: " + blobInfo.key);
        }
        return blobProvider.readBlob(blobInfo);
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the blob is managed and already uses the provider that's expected for this blob and document, there is no need
     * to recompute a key. Otherwise, go through the blob provider.
     */
    @Override
    public String writeBlob(Blob blob, Document doc) throws IOException {
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
            dispatch = blobDispatcher.getBlobProvider(blob, doc);
            if (dispatch.providerId.equals(currentProviderId)) {
                // same provider, just reuse the key
                return managedBlob.getKey();
            }
        }
        if (dispatch == null) {
            dispatch = blobDispatcher.getBlobProvider(blob, doc);
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

    protected ExtendedBlobProvider getExtendedBlobProvider(Blob blob) {
        if (!(blob instanceof ManagedBlob)) {
            return null;
        }
        ManagedBlob managedBlob = (ManagedBlob) blob;
        BlobProvider blobProvider = getBlobProvider(managedBlob.getProviderId());
        if (blobProvider == null) {
            log.error("No registered blob provider for key: " + managedBlob.getKey());
            return null;
        }
        if (!(blobProvider instanceof ExtendedBlobProvider)) {
            return null;
        }
        return (ExtendedBlobProvider) blobProvider;
    }

    @Override
    public InputStream getStream(Blob blob) throws IOException {
        ExtendedBlobProvider blobProvider = getExtendedBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getStream((ManagedBlob) blob);
    }

    @Override
    public InputStream getThumbnail(Blob blob) throws IOException {
        ExtendedBlobProvider blobProvider = getExtendedBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getThumbnail((ManagedBlob) blob);
    }

    @Override
    public URI getURI(Blob blob, UsageHint hint) throws IOException {
        ExtendedBlobProvider blobProvider = getExtendedBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getURI((ManagedBlob) blob, hint);
    }

    @Override
    public Map<String, URI> getAvailableConversions(Blob blob, UsageHint hint) throws IOException {
        ExtendedBlobProvider blobProvider = getExtendedBlobProvider(blob);
        if (blobProvider == null) {
            return Collections.emptyMap();
        }
        return blobProvider.getAvailableConversions((ManagedBlob) blob, hint);
    }

    @Override
    public InputStream getConvertedStream(Blob blob, String mimeType) throws IOException {
        ExtendedBlobProvider blobProvider = getExtendedBlobProvider(blob);
        if (blobProvider == null) {
            return null;
        }
        return blobProvider.getConvertedStream((ManagedBlob) blob, mimeType);
    }

    protected void freezeVersion(BlobAccessor accessor) {
        Blob blob = accessor.getBlob();
        ExtendedBlobProvider blobProvider = getExtendedBlobProvider(blob);
        if (blobProvider == null) {
            return;
        }
        try {
            Blob newBlob = blobProvider.freezeVersion((ManagedBlob) blob);
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
    public void freezeVersion(Document doc) throws DocumentException {
        // finds all blobs, then ask their providers if there's anything to do on check in
        doc.visitBlobs(this::freezeVersion);
    }

}
