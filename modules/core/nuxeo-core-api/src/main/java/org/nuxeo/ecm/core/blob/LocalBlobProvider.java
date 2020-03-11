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
import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple blob provider storing blobs on the local filesystem.
 *
 * @since 11.1
 */
public class LocalBlobProvider extends BlobStoreBlobProvider {

    private static final Logger log = LogManager.getLogger(LocalBlobProvider.class);

    protected LocalBlobStoreConfiguration config;

    @Override
    protected BlobStore getBlobStore(String blobProviderId, Map<String, String> properties) throws IOException {
        config = new LocalBlobStoreConfiguration(properties);
        log.info("Registering blob provider '" + blobProviderId + "' using directory: " + config.storageDir.getParent());
        KeyStrategy keyStrategy = getKeyStrategy();
        PathStrategy pathStrategy;
        if (keyStrategy.useDeDuplication()) {
            pathStrategy = new PathStrategySubDirs(config.storageDir, config.descriptor.depth);
        } else {
            pathStrategy = new PathStrategyFlat(config.storageDir);
        }
        BlobStore store = newBlobStore("File", keyStrategy, pathStrategy);
        if (isTransactional()) {
            PathStrategy transientPathStrategy = new PathStrategyFlat(config.tmpDir);
            BlobStore transientStore = new LocalBlobStore("File_tmp", keyStrategy, transientPathStrategy);
            store = new TransactionalBlobStore(store, transientStore);
        }
        return store;
    }

    protected BlobStore newBlobStore(String name, KeyStrategy keyStrategy, PathStrategy pathStrategy) {
        return new LocalBlobStore(name, keyStrategy, pathStrategy);
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    protected String getDigestAlgorithm() {
        return config.digestConfiguration.digestAlgorithm;
    }

    // used by DownloadServiceImpl for accelerated download
    public Path getStorageDir() {
        return config.storageDir;
    }

}
