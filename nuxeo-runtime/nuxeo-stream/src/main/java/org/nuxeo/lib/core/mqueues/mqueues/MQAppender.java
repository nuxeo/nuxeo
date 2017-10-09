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
package org.nuxeo.lib.core.mqueues.mqueues;

import java.io.Externalizable;
import java.time.Duration;
import java.util.Objects;

/**
 * An appender is used to append message into a MQueue.
 *
 * Implementations must be thread safe.
 *
 * @since 9.2
 */
public interface MQAppender<M extends Externalizable> extends AutoCloseable {

    /**
     * Returns the MQueue's name.
     */
    String name();

    /**
     * Returns the number of partitions.
     */
    int size();

    /**
     * Append a message into a partition, returns {@link MQOffset} position of the message.
     *
     * This method is thread safe, a queue can be shared by multiple producers.
     *
     * @param partition index lower than {@link #size()}
     */
    MQOffset append(int partition, M message);

    /**
     * Same as {@link #append(int, Externalizable)}, the queue is chosen using a hash of {@param key}.
     */
    default MQOffset append(String key, M message) {
        Objects.requireNonNull(key);
        // Provide a basic partitioning that works because:
        // 1. String.hashCode is known to be constant even with different JVM (this is not the case for all objects)
        // 2. the modulo operator is not optimal when rebalancing on partitions resizing but this should not happen.
        // and yes hashCode can be negative.
        int queue = (key.hashCode() & 0x7fffffff) % size();
        return append(queue, message);
    }

    /**
     * Wait for consumer to process a message up to the offset.
     *
     * The message is processed if a consumer of the group commits a greater or equals offset.
     *
     * Return {@code true} if the message has been consumed, {@code false} in case of timeout.
     */
    boolean waitFor(MQOffset offset, String group, Duration timeout) throws InterruptedException;

    /**
     * Returns {@code true} if the appender has been closed.
     */
    boolean closed();

    @Override
    void close();
}
