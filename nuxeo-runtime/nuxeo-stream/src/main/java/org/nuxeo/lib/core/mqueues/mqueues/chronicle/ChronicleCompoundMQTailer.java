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
package org.nuxeo.lib.core.mqueues.mqueues.chronicle;

import org.nuxeo.lib.core.mqueues.mqueues.MQOffset;
import org.nuxeo.lib.core.mqueues.mqueues.MQPartition;
import org.nuxeo.lib.core.mqueues.mqueues.MQRecord;
import org.nuxeo.lib.core.mqueues.mqueues.MQTailer;

import java.io.Externalizable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * A compound tailer to handle multiple partitions.
 *
 * @since 9.2
 */
public class ChronicleCompoundMQTailer<M extends Externalizable> implements MQTailer<M> {
    protected final List<ChronicleMQTailer<M>> tailers = new ArrayList<>();
    protected final String group;
    protected final int size;
    protected final List<MQPartition> mqPartitions = new ArrayList<>();
    protected boolean closed = false;
    protected long counter = 0;

    public ChronicleCompoundMQTailer(Collection<ChronicleMQTailer<M>> tailers, String group) {
        // empty tailers is an accepted input
        this.tailers.addAll(tailers);
        this.group = group;
        this.size = tailers.size();
        tailers.forEach(partition -> mqPartitions.addAll(partition.assignments()));
    }

    @Override
    public MQRecord<M> read(Duration timeout) throws InterruptedException {
        MQRecord<M> ret = read();
        if (ret != null) {
            return ret;
        }
        final long timeoutMs = timeout.toMillis();
        final long deadline = System.currentTimeMillis() + timeoutMs;
        final long delay = Math.min(ChronicleMQTailer.POLL_INTERVAL_MS, timeoutMs);
        while (ret == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(delay);
            ret = read();
        }
        return ret;
    }

    protected MQRecord<M> read() {
        if (size <= 0) {
            return null;
        }
        // round robin on tailers
        MQRecord<M> ret;
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
    public MQOffset commit(MQPartition partition) {
        for (MQTailer<M> tailer : tailers) {
            if (tailer.assignments().contains(partition)) {
                return tailer.commit(partition);
            }
        }
        throw new IllegalArgumentException("No tailer matching: " + partition);
    }

    @Override
    public void commit() {
        tailers.forEach(MQTailer::commit);
    }

    @Override
    public void toEnd() {
        tailers.forEach(ChronicleMQTailer::toEnd);
    }

    @Override
    public void toStart() {
        tailers.forEach(ChronicleMQTailer::toStart);
    }

    @Override
    public void toLastCommitted() {
        tailers.forEach(ChronicleMQTailer::toLastCommitted);
    }

    @Override
    public Collection<MQPartition> assignments() {
        return mqPartitions;
    }

    @Override
    public String group() {
        return group;
    }

    @Override
    public boolean closed() {
        return closed;
    }

    @Override
    public void seek(MQOffset offset) {
        for (MQTailer<M> tailer : tailers) {
            if (tailer.assignments().contains(offset.partition())) {
                tailer.seek(offset);
                return;
            }
        }
        // Should be an IllegalArgumentException but Kafka raise a state exception so do the same
        throw new IllegalStateException("Can not seek, tailer " + this + " has no assignment for partition: " + offset);
    }

    @Override
    public void reset() {
        tailers.forEach(ChronicleMQTailer::reset);
    }

    @Override
    public void reset(MQPartition partition) {
        ChronicleMQTailer<M> tailer = tailers.stream().filter(t -> t.assignments().contains(partition)).findFirst().orElse(null);
        if (tailer == null) {
            throw new IllegalArgumentException(String.format("Can not reset, partition: %s not found on tailer assignments: %s",
                    partition, mqPartitions));
        }
    }

    @Override
    public void close() {
        for (ChronicleMQTailer<M> tailer : tailers) {
            tailer.close();
        }
        closed = true;
    }
}
