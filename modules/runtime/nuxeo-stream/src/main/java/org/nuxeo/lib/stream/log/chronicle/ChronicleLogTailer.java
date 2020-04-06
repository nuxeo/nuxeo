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
package org.nuxeo.lib.stream.log.chronicle;

import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;
import static org.nuxeo.lib.stream.log.chronicle.ChronicleLogAppender.MSG_KEY;

import java.io.Externalizable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.internals.LogOffsetImpl;
import org.nuxeo.lib.stream.log.internals.LogPartitionGroup;

import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerState;

/**
 * @since 9.3
 */
public class ChronicleLogTailer<M extends Externalizable> implements LogTailer<M> {
    protected static final long POLL_INTERVAL_MS = 100L;

    // keep track of all tailers on the same namespace index even from different log
    protected static final Set<LogPartitionGroup> tailersId = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final Log log = LogFactory.getLog(ChronicleLogTailer.class);

    protected final String basePath;

    protected final ExcerptTailer cqTailer;

    protected final ChronicleLogOffsetTracker offsetTracker;

    protected final LogPartitionGroup id;

    protected final LogPartition partition;

    protected final Codec<M> codec;

    protected volatile boolean closed = false;

    public ChronicleLogTailer(Codec<M> codec, String basePath, ExcerptTailer cqTailer, LogPartition partition,
                              Name group, ChronicleRetentionDuration retention) {
        Objects.requireNonNull(group);
        this.codec = codec;
        this.basePath = basePath;
        this.cqTailer = cqTailer;
        this.partition = partition;
        this.id = new LogPartitionGroup(group, partition.name(), partition.partition());
        registerTailer();
        this.offsetTracker = new ChronicleLogOffsetTracker(basePath, partition.partition(), group, retention);
        toLastCommitted();
    }

    protected void registerTailer() {
        if (!tailersId.add(id)) {
            throw new IllegalArgumentException("A tailer for this queue and namespace already exists: " + id);
        }
    }

    protected void unregisterTailer() {
        tailersId.remove(id);
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
        List<M> value = new ArrayList<>(1);
        AtomicLong offset = new AtomicLong();
        if (NO_CODEC.equals(codec)) {
            // default format to keep backward compatibility
            try {
                if (!cqTailer.readDocument(w -> {
                    offset.set(cqTailer.index());
                    value.add((M) w.read(MSG_KEY).object());
                })) {
                    return null;
                }
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            if (!cqTailer.readDocument(w -> {
                offset.set(cqTailer.index());
                value.add(codec.decode(w.read().bytes()));
            })) {
                return null;
            }
        }
        return new LogRecord<>(value.get(0), new LogOffsetImpl(partition, offset.get()));
    }

    @Override
    public LogOffset commit(LogPartition partition) {
        if (!this.partition.equals(partition)) {
            throw new IllegalArgumentException("Cannot commit this partition: " + partition + " from " + id);
        }
        long offset = cqTailer.index();
        offsetTracker.commit(offset);
        if (log.isTraceEnabled()) {
            log.trace(String.format("Commit %s:+%d", id, offset));
        }
        return new LogOffsetImpl(partition, offset);
    }

    @Override
    public void commit() {
        commit(partition);
    }

    @Override
    public void toEnd() {
        log.debug(String.format("toEnd: %s", id));
        cqTailer.toEnd();
    }

    @Override
    public void toStart() {
        log.debug(String.format("toStart: %s", id));
        cqTailer.toStart();
        if (!cqTailer.state().equals(TailerState.FOUND_CYCLE)) {
            log.info("Unable to move to start because the tailer is not initialized, " + this);
        }
    }

    @Override
    public void toLastCommitted() {
        long offset = offsetTracker.getLastCommittedOffset();
        if (offset > 0) {
            log.debug(String.format("toLastCommitted: %s, found: %d", id, offset));
            if (!cqTailer.moveToIndex(offset) && cqTailer.index() != offset) {
                // sometime moveToIndex returns false but offset is moved
                toStart();
                long startOffset = cqTailer.index();
                if (offset < startOffset) {
                    log.error("The last committed offset: " + offset + " for tailer: " + this
                            + " points to a record that has been deleted by the retention policy."
                            + " Records have been lost, continuing from the beginning of the partition offset: "
                            + startOffset);
                } else {
                    // probably a corrupted Log where some cq4 files are missing
                    throw new IllegalStateException(
                            "Unable to move to the last committed offset: " + offset + " for tailer: " + this);
                }
            }
        } else {
            log.debug(String.format("toLastCommitted: %s, not found, move toStart", id));
            cqTailer.toStart();
        }
    }

    @Override
    public void seek(LogOffset offset) {
        if (!this.partition.equals(offset.partition())) {
            throw new IllegalStateException(
                    "Cannot seek, tailer " + this + " has no assignment for partition: " + offset);
        }
        log.debug("Seek to " + offset + " from tailer: " + this);
        if (!cqTailer.moveToIndex(offset.offset()) && cqTailer.index() != offset.offset()) {
            throw new IllegalStateException("Unable to seek to offset, " + this + " offset: " + offset);
        }
    }

    @Override
    public void reset() {
        reset(new LogPartition(id.name, id.partition));
    }

    @Override
    public void reset(LogPartition partition) {
        if (!this.partition.equals(partition)) {
            throw new IllegalArgumentException("Cannot reset this partition: " + partition + " from " + id);
        }
        log.info("Reset offset for partition: " + partition + " from tailer: " + this);
        cqTailer.toStart();
        commit(partition);
    }

    @Override
    public LogOffset offsetForTimestamp(LogPartition partition, long timestamp) {
        throw new UnsupportedOperationException("ChronicleLog does not support seek by timestamp");
    }

    @Override
    public Collection<LogPartition> assignments() {
        return Collections.singletonList(new LogPartition(id.name, id.partition));
    }

    @Override
    public Name group() {
        return id.group;
    }

    @Override
    public void close() {
        if (!closed) {
            log.debug("Closing: " + toString());
            offsetTracker.close();
            unregisterTailer();
            closed = true;
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
        return "ChronicleLogTailer{" + "basePath='" + basePath + '\'' + ", id=" + id + ", closed=" + closed + ", codec="
                + codec + '}';
    }

}
