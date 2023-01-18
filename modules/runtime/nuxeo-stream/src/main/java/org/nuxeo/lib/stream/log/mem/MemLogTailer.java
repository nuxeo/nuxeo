/*
 * (C) Copyright 2022 Nuxeo.
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
 */
package org.nuxeo.lib.stream.log.mem;

import static java.util.Objects.requireNonNull;
import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;
import static org.nuxeo.lib.stream.log.mem.MemLogAppender.NOCODEC_TAG;

import java.io.Externalizable;
import java.time.Duration;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;
import org.nuxeo.lib.stream.log.internals.LogPartitionGroup;
import org.nuxeo.lib.stream.log.mem.MemLogPartition.BytesAndOffset;
import org.nuxeo.lib.stream.log.mem.MemLogPartition.MemPartitionTailer;

/**
 * Memory implementation of LogTailer.
 */
public class MemLogTailer<M extends Externalizable> implements LogTailer<M> {

    private static final Logger log = LogManager.getLogger(MemLogTailer.class);

    protected static final long POLL_INTERVAL_MS = 100L;

    private final Codec<M> codec;

    private final LogPartition partition;

    private final MemPartitionTailer tailer;

    private final LogPartitionGroup lpg;

    private boolean initialized;

    private volatile boolean closed;

    public MemLogTailer(MemLog log, LogPartition partition, Name group, Codec<M> codec) {
        requireNonNull(group);
        this.codec = codec;
        this.partition = partition;
        tailer = log.getPartition(partition.partition()).createTailer(group);
        lpg = new LogPartitionGroup(group, partition.name(), partition.partition());
    }

    protected void checkInitialized() {
        if (initialized) {
            return;
        }
        toLastCommitted();
    }

    @Override
    public LogRecord<M> read(Duration timeout) throws InterruptedException {
        LogRecord<M> ret = read();
        if (ret != null) {
            return ret;
        }
        long timeoutMs = timeout.toMillis();
        long deadline = System.currentTimeMillis() + timeoutMs;
        long delay = Math.min(POLL_INTERVAL_MS, timeoutMs);
        while (ret == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(delay);
            ret = read();
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    protected LogRecord<M> read() {
        if (closed) {
            throw new IllegalStateException("The tailer has been closed.");
        }
        checkInitialized();
        String tag = NO_CODEC.equals(codec) ? NOCODEC_TAG : null;
        BytesAndOffset bo = tailer.read(tag);
        if (bo == null) {
            return null;
        }
        long offset = bo.offset();
        M value;
        if (tag != null) {
            // default format to keep backward compatibility
            try {
                value = (M) SerializationUtils.deserialize(bo.bytes());
            } catch (SerializationException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            value = (M) codec.decode(bo.bytes());
        }
        return new LogRecord<>(value, new LogOffsetImpl(partition, offset));
    }

    @Override
    public LogOffset commit(LogPartition partition) {
        checkInitialized();
        if (!this.partition.equals(partition)) {
            throw new IllegalArgumentException("Cannot commit this partition: " + partition + " from " + lpg);
        }
        long offset = tailer.offset();
        tailer.commit(offset);
        log.trace("Commit {}:+{}", lpg, offset);
        return new LogOffsetImpl(partition, offset);
    }

    @Override
    public void commit() {
        commit(partition);
    }

    @Override
    public void toEnd() {
        log.debug("toEnd: {}", lpg);
        tailer.toEnd();
        initialized = true;
    }

    @Override
    public void toStart() {
        log.debug("toStart: {}", lpg);
        tailer.toStart();
        initialized = true;
    }

    @Override
    public void toLastCommitted() {
        long offset = tailer.committed();
        log.debug("toLastCommitted: {}, found: {}", lpg, offset);
        tailer.moveToOffset(offset);
        initialized = true;
    }

    @Override
    public void seek(LogOffset offset) {
        if (!partition.equals(offset.partition())) {
            throw new IllegalStateException(
                    "Cannot seek, tailer " + this + " has no assignment for partition: " + offset);
        }
        log.debug("Seek to {} from tailer: {}", offset, this);
        if (!tailer.moveToOffset(offset.offset()) && tailer.offset() != offset.offset()) {
            throw new IllegalStateException("Unable to seek to offset, " + this + " offset: " + offset);
        }
        initialized = true;
    }

    @Override
    public void reset() {
        reset(partition);
    }

    @Override
    public void reset(LogPartition partition) {
        if (!this.partition.equals(partition)) {
            throw new IllegalArgumentException("Cannot reset this partition: " + partition + " from " + lpg);
        }
        log.debug("Reset offset for partition: {} from tailer: {}", partition, this);
        tailer.toStart();
        initialized = true;
        commit(partition);
    }

    @Override
    public LogOffset offsetForTimestamp(LogPartition partition, long timestamp) {
        throw new UnsupportedOperationException("Mem Log does not support seek by timestamp");
    }

    @Override
    public Collection<LogPartition> assignments() {
        return List.of(partition);
    }

    @Override
    public Name group() {
        return lpg.group;
    }

    @Override
    public void close() {
        if (!closed) {
            log.debug("Closing: {}", this);
            tailer.close();
            closed = true;
            initialized = false;
        }
    }

    @Override
    public boolean closed() {
        return closed;
    }

    @Override
    public Codec<M> getCodec() {
        return codec;
    }

    @Override
    public String toString() {
        return "MemLogTailer{id=" + lpg + ", closed=" + closed + ", codec=" + codec + '}';
    }

}
