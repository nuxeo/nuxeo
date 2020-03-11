/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.blob.binary;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * A binary manager stores binaries according to their digest.
 */
public interface BinaryManager {

    /** In the initialization properties, the property for the store path. */
    String PROP_PATH = "path";

    /** In the initialization properties, the property for a generic key. */
    String PROP_KEY = "key";

    /**
     * Initializes the binary manager.
     *
     * @param blobProviderId the blob provider id for this binary manager
     * @param properties initialization properties
     * @since 7.3
     */
    void initialize(String blobProviderId, Map<String, String> properties) throws IOException;

    /**
     * Saves the given blob into a {@link Binary}.
     * <p>
     * Returns a {@link Binary} representing the stream. The {@link Binary} includes a digest that is a sufficient
     * representation to persist it.
     * <p>
     * If the blob is a temporary {@link FileBlob}, then the temporary file may be reused as the final storage location
     * after being moved.
     *
     * @param blob the blob
     * @return the corresponding binary
     * @throws IOException
     * @since 7.2
     */
    Binary getBinary(Blob blob) throws IOException;

    /**
     * Returns a {@link Binary} corresponding to the given digest.
     * <p>
     * A {@code null} is returned if the digest could not be found.
     *
     * @param digest the digest, or {@code null}
     * @return the corresponding binary
     */
    Binary getBinary(String digest);

    /**
     * Remove definitively a set of binaries
     *
     * @since 7.10
     * @param digests a set of digests, must not be {@code null}.
     */
    void removeBinaries(Collection<String> digests);

    /**
     * Returns the Binary Garbage Collector that can be used for this binary manager.
     * <p>
     * Several calls to this method will return the same GC, so that its status can be monitored using
     * {@link BinaryGarbageCollector#isInProgress}.
     *
     * @return the binary GC
     */
    BinaryGarbageCollector getGarbageCollector();

    /**
     * Closes the binary manager and releases all resources and temporary objects held by it.
     */
    void close();

    /**
     * Returns the digest algorithm used to store and digest binaries.
     *
     * @since 7.4
     */
    String getDigestAlgorithm();

}
