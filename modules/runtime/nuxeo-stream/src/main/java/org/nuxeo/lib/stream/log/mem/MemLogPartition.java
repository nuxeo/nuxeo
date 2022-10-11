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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.nuxeo.lib.stream.log.Name;

import com.google.common.base.Objects;

/**
 * Memory implementation of Log partition.
 */
public class MemLogPartition {

    // group -> committed offset tracker
    private final Map<Name, OffsetTracker> offsetTrackers = new ConcurrentHashMap<>();

    // group -> tailer
    private final Map<Name, MemPartitionTailer> tailers = new ConcurrentHashMap<>();

    private record Message(String tag, byte[] bytes) { // NOSONAR (doesn't need equals/hashCode)
    }

    public record BytesAndOffset(byte[] bytes, long offset) { // NOSONAR (doesn't need equals/hashCode)
    }

    /** Tracks the committed offset for a group. */
    public static class OffsetTracker {

        private final ReentrantLock lock;

        private final Condition changed;

        private volatile long offset;

        public OffsetTracker() {
            lock = new ReentrantLock();
            changed = lock.newCondition();
        }

        public long get() {
            return offset;
        }

        public void set(long offset) {
            this.offset = offset;
            lock.lock();
            try {
                changed.signal();
            } finally {
                lock.unlock();
            }
        }

        public long awaitNanos(long nanos) throws InterruptedException {
            lock.lock();
            try {
                return changed.awaitNanos(nanos);
            } finally {
                lock.unlock();
            }
        }
    }

    // used synchronized
    private final List<Message> partition = new ArrayList<>();

    public MemLogPartition() {
    }

    public long append(String tag, byte[] bytes) {
        Message msg = new Message(tag, bytes);
        synchronized (partition) {
            partition.add(msg);
            return partition.size() - 1L;
        }
    }

    public long size() {
        synchronized (partition) {
            return partition.size();
        }
    }

    public OffsetTracker getCommittedOffsetTracker(Name group) {
        return offsetTrackers.computeIfAbsent(group, k -> new OffsetTracker());
    }

    public MemPartitionTailer createTailer(Name group) {
        MutableBoolean created = new MutableBoolean();
        MemPartitionTailer tailer = tailers.computeIfAbsent(group, k -> {
            created.setTrue();
            return new MemPartitionTailer(group, getCommittedOffsetTracker(group));
        });
        if (created.isFalse()) {
            throw new IllegalArgumentException("Tailer already exists: " + group);
        }
        return tailer;
    }

    public void closeTailer(Name group) {
        // note that we don't reset the committed offset
        tailers.remove(group);
    }

    public Set<Name> getGroups() {
        return offsetTrackers.keySet();
    }

    public long committed(Name group) {
        OffsetTracker co = offsetTrackers.get(group);
        return co == null ? 0 : co.get();
    }

    /**
     * A tailer for a partition maintains an offset (current reading position).
     */
    public class MemPartitionTailer {

        private final Name group;

        private final OffsetTracker offsetTracker;

        private long offset;

        public MemPartitionTailer(Name group, OffsetTracker offsetTracker) {
            this.group = group;
            this.offsetTracker = offsetTracker;
        }

        /**
         * Reads a message from the partition and increments the current offset.
         *
         * @param tag the required tag for the message
         * @return the message, or {@code null} if there is no message at the current offset
         * @throws IllegalStateException if the tag does not match
         */
        public BytesAndOffset read(String tag) {
            Message msg;
            synchronized (partition) {
                try {
                    msg = partition.get((int) offset);
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
            }
            if (!Objects.equal(tag, msg.tag())) {
                throw new IllegalArgumentException("bad tag");
            }
            BytesAndOffset res = new BytesAndOffset(msg.bytes(), offset);
            offset++;
            return res;
        }

        public long offset() {
            return offset;
        }

        public void toStart() {
            offset = 0;
        }

        public void toEnd() {
            offset = size();
        }

        public boolean moveToOffset(long offset) {
            if (offset < 0 || offset > size()) {
                return false;
            }
            this.offset = offset;
            return true;
        }

        public void commit(long committed) {
            offsetTracker.set(committed);
        }

        public long committed() {
            return offsetTracker.get();
        }

        public void close() {
            MemLogPartition.this.closeTailer(group);
        }
    }

}
