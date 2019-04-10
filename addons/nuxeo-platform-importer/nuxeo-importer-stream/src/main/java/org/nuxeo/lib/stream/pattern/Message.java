/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.pattern;

import java.io.Externalizable;

/**
 * A message is an {@link Externalizable} with an identifier.
 */
public interface Message extends Externalizable {

    /**
     * A message identifier.
     */
    String getId();

    /**
     * A consumer reading this message must not wait for new message to process the batch.
     */
    default boolean forceBatch() {
        return false;
    }

    /**
     * This message is a poison pill it contains no other data, a consumer reading this message will process the batch
     * and stop.
     */
    default boolean poisonPill() {
        return false;
    }

}
