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
package org.nuxeo.lib.stream.log;

import java.io.Externalizable;
import java.time.Duration;
import java.util.Collection;

import org.nuxeo.lib.stream.codec.Codec;

/**
 * Sequential reader for a partition or multiple partitions. A tailer is not thread safe and should not be shared by
 * multiple threads.
 */
public interface LogTailer<M extends Externalizable> extends AutoCloseable {

    /**
     * Returns the consumer group.
     */
    Name group();

    /**
     * Returns the list of Log name, partitions tuples currently assigned to this tailer. Assignments can change only if
     * the tailer has been created using {@link LogManager#subscribe}.
     */
    Collection<LogPartition> assignments();

    /**
     * Read a message from assigned partitions within the timeout.
     *
     * @return null if there is no message in the queue after the timeout.
     * @throws RebalanceException if a partition rebalancing happen during the read, this is possible only when using
     *             {@link LogManager#subscribe}.
     */
    LogRecord<M> read(Duration timeout) throws InterruptedException;

    /**
     * Commit current positions for all partitions (last message offset returned by read).
     */
    void commit();

    /**
     * Commit current position for the partition.
     *
     * @return the committed offset, can return null if there was no previous read done on this partition.
     */
    LogOffset commit(LogPartition partition);

    /**
     * Set the current positions to the end of all partitions.
     */
    void toEnd();

    /**
     * Set the current positions to the beginning of all partitions.
     */
    void toStart();

    /**
     * Set the current positions to previously committed positions.
     */
    void toLastCommitted();

    /**
     * Set the current position for a single partition. Do not change other partitions positions.
     *
     * @since 9.3
     */
    void seek(LogOffset offset);

    /**
     * Look up the offset for the given partition by timestamp. The position is the earliest offset whose timestamp is
     * greater than or equal to the given timestamp.
     * <p/>
     * The timestamp used depends on the implementation, for Kafka this is the LogAppendTime. Returns null if no record
     * offset is found with an appropriate timestamp.
     *
     * @since 10.1
     */
    LogOffset offsetForTimestamp(LogPartition partition, long timestamp);

    /**
     * Reset all committed positions for this group, next read will be done from beginning.
     *
     * @since 9.3
     */
    void reset();

    /**
     * Reset the committed position for this group on this partition, next read for this partition will be done from the
     * beginning.
     *
     * @since 9.3
     */
    void reset(LogPartition partition);

    @Override
    void close();

    /**
     * Returns {@code true} if the tailer has been closed.
     */
    boolean closed();

    /**
     * Returns the codec used to read the records. A null codec is the default legacy encoding.
     *
     * @since 10.2
     */
    Codec<M> getCodec();
}
