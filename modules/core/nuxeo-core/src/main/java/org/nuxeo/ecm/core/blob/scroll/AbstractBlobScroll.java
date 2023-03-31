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
package org.nuxeo.ecm.core.blob.scroll;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.KeyStrategyDigest;
import org.nuxeo.ecm.core.scroll.GenericScrollRequest;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 2023
 */
public abstract class AbstractBlobScroll<T extends BlobStoreBlobProvider> implements Scroll {

    private static final Logger log = LogManager.getLogger(AbstractBlobScroll.class);

    protected boolean isKeyPrefixed;

    protected KeyStrategy keyStrategy;

    protected String providerId;

    protected int size;

    protected Function<String, Boolean> validate;

    protected long totalBlobCount;

    protected long totalBlobSizeCount;

    @SuppressWarnings("unchecked")
    public void init(ScrollRequest request, Map<String, String> options) {
        this.totalBlobCount = 0;
        this.totalBlobSizeCount = 0;
        if (!(request instanceof GenericScrollRequest)) {
            throw new IllegalArgumentException(
                    "Requires a GenericScrollRequest got a " + request.getClass().getCanonicalName());
        }
        GenericScrollRequest scrollRequest = (GenericScrollRequest) request;
        providerId = scrollRequest.getQuery();
        BlobProvider blobProvider = Framework.getService(BlobManager.class).getBlobProvider(providerId);
        if (!(blobProvider instanceof BlobStoreBlobProvider)) {
            throw new UnsupportedOperationException(providerId + " is not a BlobStoreBlobProvider");
        }
        BlobStoreBlobProvider blobStoreBlobProvider = (BlobStoreBlobProvider) blobProvider;
        this.size = scrollRequest.getSize();
        isKeyPrefixed = !Framework.getService(DocumentBlobManager.class).isUseRepositoryName();
        keyStrategy = blobStoreBlobProvider.getKeyStrategy();
        if (keyStrategy instanceof KeyStrategyDigest) {
            // skip blob keys that does not look like digests
            this.validate = (key) -> ((KeyStrategyDigest) keyStrategy).isValidDigest(key);
        }
        init((T) blobStoreBlobProvider);
    }

    protected boolean addTo(List<String> list, String key) {
        return addTo(list, key, null);
    }

    /**
     * Adds the blob key to the list if it is valid (e.g. looks like a digest if the provider has digest strategy). The
     * key added to the list will be prefixed with the provider id if it has to be.
     *
     * @param list the list to be
     * @param key the blob key
     * @param getSize supplier to sum the total size
     * @return true if the key was added to the list
     */
    protected boolean addTo(List<String> list, String key, Supplier<Long> getSize) {
        if (validate != null && !validate.apply(key)) {
            return false;
        }
        if (isKeyPrefixed) {
            key = providerId + ":" + key;
        }
        list.add(key);
        totalBlobCount++;
        if (getSize != null) {
            totalBlobSizeCount += getSize.get();
        }
        return true;
    }

    public void close() {
        log.info("Scrolled {} objects of total size {}", () -> totalBlobCount,
                () -> FileUtils.byteCountToDisplaySize(totalBlobSizeCount));
    }

    protected abstract void init(T provider);

}
