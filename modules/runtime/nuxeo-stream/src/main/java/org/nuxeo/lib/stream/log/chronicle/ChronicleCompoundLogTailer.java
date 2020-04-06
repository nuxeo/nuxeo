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

import java.io.Externalizable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;

/**
 * A compound tailer to handle multiple partitions.
 *
 * @since 9.3
 */
public class ChronicleCompoundLogTailer<M extends Externalizable> implements LogTailer<M> {
    protected final List<ChronicleLogTailer<M>> tailers = new ArrayList<>();

    protected final Name group;

    protected final int size;

    protected final List<LogPartition> logPartitions = new ArrayList<>();

    protected final Codec<M> codec;

    protected boolean closed;

    protected long counter;

    public ChronicleCompoundLogTailer(Collection<ChronicleLogTailer<M>> tailers, Name group) {
        // empty tailers is an accepted input
        this.tailers.addAll(tailers);
        this.group = group;
        this.size = tailers.size();
        if (tailers.isEmpty()) {
            this.codec = null;
        } else {
            this.codec = tailers.iterator().next().getCodec();
        }
        tailers.forEach(partition -> logPartitions.addAll(partition.assignments()));
    }

    @Override
    public LogRecord<M> read(Duration timeout) throws InterruptedException {
        LogRecord<M> ret = read();
        if (ret != null) {
            return ret;
        }
        final long timeoutMs = timeout.toMillis();
        final long deadline = System.currentTimeMillis() + timeoutMs;
        final long delay = Math.min(ChronicleLogTailer.POLL_INTERVAL_MS, timeoutMs);
        while (ret == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(delay);
            ret = read();
        }
        return ret;
    }

    protected LogRecord<M> read() {
        if (size <= 0) {
            return null;
        }
        // round robin on tailers
        LogRecord<M> ret;
        long end = counter + size;
        do {
            counter++;
            int i = (int) counter % size;
            ret = tailers.get(i).read();
            if (ret != null) {
                return ret;
            }
        } while (counter < end);
        return null;
    }

    @Override
    public LogOffset commit(LogPartition partition) {
        for (LogTailer<M> tailer : tailers) {
            if (tailer.assignments().contains(partition)) {
                return tailer.commit(partition);
            }
        }
        throw new IllegalArgumentException("No tailer matching: " + partition);
    }

    @Override
    public void commit() {
        tailers.forEach(LogTailer::commit);
    }

    @Override
    public void toEnd() {
        tailers.forEach(ChronicleLogTailer::toEnd);
    }

    @Override
    public void toStart() {
        tailers.forEach(ChronicleLogTailer::toStart);
    }

    @Override
    public void toLastCommitted() {
        tailers.forEach(ChronicleLogTailer::toLastCommitted);
    }

    @Override
    public Collection<LogPartition> assignments() {
        return logPartitions;
    }

    @Override
    public Name group() {
        return group;
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
    public void seek(LogOffset offset) {
        for (LogTailer<M> tailer : tailers) {
            if (tailer.assignments().contains(offset.partition())) {
                tailer.seek(offset);
                return;
            }
        }
        // Should be an IllegalArgumentException but Kafka raise a state exception so do the same
        throw new IllegalStateException("Cannot seek, tailer " + this + " has no assignment for partition: " + offset);
    }

    @Override
    public LogOffset offsetForTimestamp(LogPartition partition, long timestamp) {
        throw new UnsupportedOperationException("ChronicleLog does not support seek by timestamp");
    }

    @Override
    public void reset() {
        tailers.forEach(ChronicleLogTailer::reset);
    }

    @Override
    public void reset(LogPartition partition) {
        ChronicleLogTailer<M> tailer = tailers.stream()
                                              .filter(t -> t.assignments().contains(partition))
                                              .findFirst()
                                              .orElseThrow(() -> new IllegalArgumentException(String.format(
                                                      "Cannot reset, partition: %s not found on tailer assignments: %s",
                                                      partition, logPartitions)));
        tailer.reset();
    }

    @Override
    public void close() {
        for (ChronicleLogTailer<M> tailer : tailers) {
            tailer.close();
        }
        closed = true;
    }

}
