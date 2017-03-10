/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.pubsub;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Generic interface for serializable invalidations.
 *
 * @since 9.1
 */
public interface SerializableInvalidations extends Serializable {

    /** Checks if this contains no invalidations. */
    boolean isEmpty();

    /** Adds other invalidations to this one. */
    void add(SerializableInvalidations other);

    /**
     * Serializes this to an output stream. The deserialization is handled by
     * {@link AbstractPubSubInvalidator#deserialize}.
     */
    void serialize(OutputStream out) throws IOException;

}
