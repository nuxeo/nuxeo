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
import java.util.Objects;
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
 * Abstract class to scroll blobs from a blob provider, the scroll query is the provider id.
 *
 * @since 2023
 */
public abstract class AbstractBlobScroll<T extends BlobStoreBlobProvider> implements Scroll {

    private static final Logger log = LogManager.getLogger(AbstractBlobScroll.class);

    protected static final String SIZE_DELIMITER = ":";

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

    /**
     * Adds the blob key to the list if it is valid (e.g. looks like a digest if the provider has digest strategy).
     * <p>
     * The key added to the list will be prefixed with the provider id if it has to be.
     * <p>
     * The key added to the list will be suffixed with the size of the associated blob with the
     * {@link org.nuxeo.ecm.core.blob.scroll.AbstractBlobScroll#SIZE_DELIMITER} separator.
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
        totalBlobCount++;
        Long size = 0L;
        size = Objects.requireNonNull(getSize, "Size supplier must not be null").get();
        totalBlobSizeCount += size;
        key = key + SIZE_DELIMITER + size;
        list.add(key);
        return true;
    }

    public void close() {
        log.info("Scrolled {} objects of total size {}", () -> totalBlobCount,
                () -> FileUtils.byteCountToDisplaySize(totalBlobSizeCount));
    }

    protected abstract void init(T provider);

    public static String getBlobKey(String id) {
        int sizeSeparator = id.lastIndexOf(SIZE_DELIMITER);
        if (sizeSeparator < 0) {
            return id;
        }
        return id.substring(0, sizeSeparator);
    }

    public static Long getBlobSize(String id) {
        int sizeSeparator = id.lastIndexOf(SIZE_DELIMITER);
        if (sizeSeparator < 0 || sizeSeparator + 1 == id.length()) {
            // if no size delimiter or is at last position
            return null;
        }
        return Long.parseLong(id.substring(sizeSeparator + 1, id.length()));
    }

}
