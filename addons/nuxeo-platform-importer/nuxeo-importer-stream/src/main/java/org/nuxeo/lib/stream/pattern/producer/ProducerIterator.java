/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.lib.stream.pattern.producer;

import java.util.Iterator;

import org.nuxeo.lib.stream.pattern.Message;

/**
 * A ProducerIterator returns {@link Message}. It also has the logic to return a partition index, that will be used to
 * run concurrent consumers.
 *
 * @since 9.1
 */
public interface ProducerIterator<M extends Message> extends Iterator<M>, AutoCloseable {

    /**
     * The remove method is not needed.
     */
    @Override
    default void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a partition associated with the {@link Message}. The value returned must be between 0 and lower than
     * partitions.
     *
     * @param message the message to shard
     * @param partitions the number of partitions
     */
    int getPartition(M message, int partitions);

}
