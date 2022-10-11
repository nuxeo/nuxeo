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

import static org.nuxeo.lib.stream.log.mem.MemLogTailer.POLL_INTERVAL_MS;

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
 */
public class MemCompoundLogTailer<M extends Externalizable> implements LogTailer<M> {

    private final List<MemLogTailer<M>> tailers;

    private final Name group;

    private final int size;

    private final List<LogPartition> logPartitions;

    private final Codec<M> codec;

    private volatile boolean closed;

    private long counter;

    @SuppressWarnings("resource")
    public MemCompoundLogTailer(List<MemLogTailer<M>> tailers, Name group) {
        // empty tailers is an accepted input
        this.tailers = new ArrayList<>(tailers);
        this.group = group;
        this.size = tailers.size();
        if (tailers.isEmpty()) {
            this.codec = null;
        } else {
            this.codec = tailers.iterator().next().getCodec();
        }
        logPartitions = tailers.stream().map(MemLogTailer::assignments).flatMap(Collection::stream).toList();
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

    // round robin on tailers
    protected LogRecord<M> read() {
        if (size == 0) {
            return null;
        }
        long end = counter + size;
        do {
            counter++;
            int i = (int) counter % size;
            @SuppressWarnings("resource")
            LogRecord<M> ret = tailers.get(i).read();
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
        tailers.forEach(MemLogTailer::toEnd);
    }

    @Override
    public void toStart() {
        tailers.forEach(MemLogTailer::toStart);
    }

    @Override
    public void toLastCommitted() {
        tailers.forEach(MemLogTailer::toLastCommitted);
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
        LogPartition partition = offset.partition();
        for (LogTailer<M> tailer : tailers) {
            if (tailer.assignments().contains(partition)) {
                tailer.seek(offset);
                return;
            }
        }
        // Should be an IllegalArgumentException but Kafka raise a state exception so do the same
        throw new IllegalStateException("Cannot seek, tailer " + this + " has no assignment for partition: " + offset);
    }

    @Override
    public LogOffset offsetForTimestamp(LogPartition partition, long timestamp) {
        throw new UnsupportedOperationException("MemLog does not support seek by timestamp");
    }

    @Override
    public void reset() {
        tailers.forEach(MemLogTailer::reset);
    }

    @SuppressWarnings("resource")
    @Override
    public void reset(LogPartition partition) {
        tailers.stream()
               .filter(t -> t.assignments().contains(partition))
               .findFirst()
               .orElseThrow(() -> new IllegalArgumentException(
                       "Cannot reset, partition: %s not found on tailer assignments: %s".formatted(partition,
                               logPartitions)))
               .reset();
    }

    @Override
    public void close() {
        tailers.forEach(MemLogTailer::close);
        closed = true;
    }

}
