/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.storage.mongodb.blob;

import java.io.IOException;
import java.util.Map;

import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.CachingBlobStore;
import org.nuxeo.ecm.core.blob.CachingConfiguration;
import org.nuxeo.ecm.core.blob.DigestConfiguration;

/**
 * Blob provider that stores files in MongoDB GridFS.
 * <p>
 * This implementation does not support transactional mode.
 *
 * @since 2023.5
 */
public class GridFSBlobProvider extends BlobStoreBlobProvider {

    public static final String SYSTEM_PROPERTY_PREFIX = "nuxeo.gridFS";

    public static final String STORE_SCROLL_NAME = "gridFSBlobScroll";

    protected DigestConfiguration digestConfiguration;

    @Override
    public void close() {
        // do nothing
    }

    @Override
    protected BlobStore getBlobStore(String blobProviderId, Map<String, String> properties) throws IOException {
        digestConfiguration = new DigestConfiguration(SYSTEM_PROPERTY_PREFIX, properties);
        BlobStore store = new GridFSBlobStore(blobProviderId, "GridFS", properties, getKeyStrategy());
        boolean caching = properties.containsKey("nocache") ? Boolean.parseBoolean(properties.get("nocache")) : true;
        if (caching) {
            CachingConfiguration cachingConfiguration = new CachingConfiguration(SYSTEM_PROPERTY_PREFIX, properties);
            store = new CachingBlobStore(blobProviderId, "Cache", store, cachingConfiguration);
        }
        return store;
    }

    @Override
    protected String getDigestAlgorithm() {
        return digestConfiguration.digestAlgorithm;
    }

    @Override
    public String getStoreScrollName() {
        return STORE_SCROLL_NAME;
    }

}
