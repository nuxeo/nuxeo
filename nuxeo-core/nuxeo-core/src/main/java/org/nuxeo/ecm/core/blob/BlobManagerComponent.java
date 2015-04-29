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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Implementation of the service managing the storage and retrieval of {@link Blob}s, through internally-registered
 * {@link BlobProvider}s.
 *
 * @since 7.2
 */
public class BlobManagerComponent extends DefaultComponent implements BlobManager {

    /** The default blob provider, for a null prefix. Used to write non-managed blobs to the repository. */
    protected BlobProvider defaultBlobProvider;

    /** Blob providers by prefix, except for the default which is stored separately. */
    protected final Map<String, BlobProvider> blobProviders = new ConcurrentHashMap<>();

    @Override
    public void activate(ComponentContext context) {
        clearRegistrations();
    }

    @Override
    public void deactivate(ComponentContext context) {
        clearRegistrations();
    }

    protected void clearRegistrations() {
        defaultBlobProvider = null;
        blobProviders.clear();
    }

    @Override
    public void registerBlobProvider(String prefix, BlobProvider blobProvider) {
        if (prefix == null) {
            if (defaultBlobProvider != null) {
                throw new NuxeoException("Already existing default provider");
            }
            defaultBlobProvider = blobProvider;
        } else {
            if (blobProviders.containsKey(prefix)) {
                throw new NuxeoException("Already existing provider: " + prefix);
            }
            blobProviders.put(prefix, blobProvider);
        }
    }

    @Override
    public void unregisterBlobProvider(String prefix) {
        if (prefix == null) {
            if (defaultBlobProvider == null) {
                throw new NuxeoException("No default provider");
            }
            defaultBlobProvider = null;

        } else {
            if (blobProviders.remove(prefix) == null) {
                throw new NuxeoException("Unknown provider: " + prefix);
            }
        }
    }

    @Override
    public BlobProvider getBlobProvider(String key) {
        // key exact match
        if (key == null) {
            return defaultBlobProvider;
        }
        BlobProvider blobProvider = blobProviders.get(key);
        if (blobProvider != null) {
            return blobProvider;
        }
        // key prefix
        int colon = key.indexOf(':');
        if (colon < 0) {
            return defaultBlobProvider;
        } else {
            String prefix = key.substring(0, colon);
            blobProvider = blobProviders.get(prefix);
            if (blobProvider == null) {
                throw new NuxeoException("Unknown blob provider for key: " + key);
            }
            return blobProvider;
        }
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo, Document doc) throws IOException {
        if (blobInfo.key == null) {
            return null;
        }
        return getBlobProvider(blobInfo.key).readBlob(blobInfo, doc);
    }

    @Override
    public BlobInfo writeBlob(Blob blob, Document doc) throws IOException {
        if (blob instanceof ManagedBlob) {
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = ((ManagedBlob) blob).getKey();
            blobInfo.mimeType = blob.getMimeType();
            blobInfo.encoding = blob.getEncoding();
            blobInfo.filename = blob.getFilename();
            blobInfo.digest = blob.getDigest();
            blobInfo.length = Long.valueOf(blob.getLength());
            return blobInfo;
        } else {
            return defaultBlobProvider.writeBlob(blob, doc);
        }
    }

    @Override
    public InputStream getStream(Blob blob) throws IOException {
        if (!(blob instanceof ManagedBlob)) {
            return null;
        }
        ManagedBlob managedBlob = (ManagedBlob) blob;
        BlobProvider blobProvider = getBlobProvider(managedBlob.getKey());
        if (!(blobProvider instanceof ExtendedBlobProvider)) {
            return null;
        }
        return ((ExtendedBlobProvider) blobProvider).getStream(managedBlob);
    }

    @Override
    public InputStream getThumbnail(Blob blob) throws IOException {
        if (!(blob instanceof ManagedBlob)) {
            return null;
        }
        ManagedBlob managedBlob = (ManagedBlob) blob;
        BlobProvider blobProvider = getBlobProvider(managedBlob.getKey());
        if (!(blobProvider instanceof ExtendedBlobProvider)) {
            return null;
        }
        return ((ExtendedBlobProvider) blobProvider).getThumbnail(managedBlob);
    }

    @Override
    public URI getURI(Blob blob, UsageHint hint) throws IOException {
        if (!(blob instanceof ManagedBlob)) {
            return null;
        }
        ManagedBlob managedBlob = (ManagedBlob) blob;
        BlobProvider blobProvider = getBlobProvider(managedBlob.getKey());
        if (!(blobProvider instanceof ExtendedBlobProvider)) {
            return null;
        }
        return ((ExtendedBlobProvider) blobProvider).getURI(managedBlob, hint);
    }

    @Override
    public Map<String, URI> getAvailableConversions(Blob blob, UsageHint hint) throws IOException {
        if (!(blob instanceof ManagedBlob)) {
            return Collections.emptyMap();
        }
        ManagedBlob managedBlob = (ManagedBlob) blob;
        BlobProvider blobProvider = getBlobProvider(managedBlob.getKey());
        if (!(blobProvider instanceof ExtendedBlobProvider)) {
            return Collections.emptyMap();
        }
        return ((ExtendedBlobProvider) blobProvider).getAvailableConversions(managedBlob, hint);
    }

    @Override
    public InputStream getConvertedStream(Blob blob, String mimeType) throws IOException {
        if (!(blob instanceof ManagedBlob)) {
            return null;
        }
        ManagedBlob managedBlob = (ManagedBlob) blob;
        BlobProvider blobProvider = getBlobProvider(managedBlob.getKey());
        if (!(blobProvider instanceof ExtendedBlobProvider)) {
            return null;
        }
        return ((ExtendedBlobProvider) blobProvider).getConvertedStream(managedBlob, mimeType);
    }

}
