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
package org.nuxeo.runtime.pubsub;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Generic interface for a serializable message.
 *
 * @since 9.3
 */
public interface SerializableMessage extends Serializable {

    /**
     * Serializes this to an output stream. The deserialization is handled by
     * {@link AbstractPubSubBroker#deserialize}.
     */
    void serialize(OutputStream out) throws IOException;

}
