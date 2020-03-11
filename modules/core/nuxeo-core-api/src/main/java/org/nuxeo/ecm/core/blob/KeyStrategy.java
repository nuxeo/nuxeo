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

import java.io.OutputStream;

/**
 * Decides how a key is computed from a blob.
 * <p>
 * Implementations of this class must have a proper {@link #equals} method.
 *
 * @since 11.1
 */
public interface KeyStrategy {

    /**
     * Checks whether this key strategy uses de-duplication. When de-duplication is used, two blobs with identical
     * contents have identical keys.
     */
    boolean useDeDuplication();

    /**
     * Gets, if possible, a digest from the key. This is not possible if the key is not derived from a digest.
     *
     * @param key the key
     * @return a digest, or {@code null}
     */
    String getDigestFromKey(String key);

    /**
     * Observer of the writes to an {@link OutputStream}.
     */
    interface WriteObserver {

        /**
         * Wraps the given stream to observe it.
         */
        OutputStream wrap(OutputStream out);

        /**
         * Must be called when writes to the wrapped stream are done, to complete observation.
         */
        void done();
    }

    /**
     * Gets the write context for the given blob.
     */
    BlobWriteContext getBlobWriteContext(BlobContext blobContext);

}
