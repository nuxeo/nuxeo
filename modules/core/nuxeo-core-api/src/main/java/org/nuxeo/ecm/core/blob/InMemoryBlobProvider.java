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
import java.util.Map;

/**
 * Blob storage in memory, mostly for unit tests.
 *
 * @since 11.1
 */
public class InMemoryBlobProvider extends BlobStoreBlobProvider {

    protected DigestConfiguration digestConfiguration;

    @Override
    protected BlobStore getBlobStore(String blobProviderId, Map<String, String> properties) throws IOException {
        digestConfiguration = new DigestConfiguration(null, properties);
        PropertyBasedConfiguration config = new PropertyBasedConfiguration(null, properties);
        KeyStrategy keyStrategy = getKeyStrategy();
        BlobStore store = new InMemoryBlobStore("mem", keyStrategy);
        if (isTransactional()) {
            BlobStore transientStore = new InMemoryBlobStore("mem_tmp", keyStrategy);
            store = new TransactionalBlobStore(store, transientStore);
        }
        if (config.getBooleanProperty("test-caching")) { // for tests
            CachingConfiguration cachingConfiguration = new CachingConfiguration(null, properties);
            store = new CachingBlobStore("Cache", store, cachingConfiguration);
        }
        return store;
    }

    @Override
    public void close() {
        // nothing
    }

    @Override
    protected String getDigestAlgorithm() {
        return digestConfiguration.digestAlgorithm;
    }

}
