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
package org.nuxeo.lib.stream.log.internals;

import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.Name;

/**
 * @since 9.3
 */
public class LogOffsetImpl implements LogOffset {
    protected final LogPartition partition;

    protected final long offset;

    public LogOffsetImpl(LogPartition partition, long offset) {
        this(partition.name(), partition.partition(), offset);
    }

    public LogOffsetImpl(Name name, int partition, long offset) {
        this.partition = LogPartition.of(name, partition);
        this.offset = offset;
    }

    @Override
    public LogPartition partition() {
        return partition;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public LogOffset nextOffset() {
        return new LogOffsetImpl(partition, offset + 1);
    }

    @Override
    public String toString() {
        return String.format("%s:+%d", partition, offset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        LogOffsetImpl offsetImpl = (LogOffsetImpl) o;

        return partition.equals(offsetImpl.partition) && offset == offsetImpl.offset;
    }

    @Override
    public int hashCode() {
        int result = partition != null ? partition.hashCode() : 0;
        result = 31 * result + (int) (offset ^ (offset >>> 32));
        return result;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(LogOffset o) {
        if (this == o)
            return 0;
        if (o == null || getClass() != o.getClass()) {
            throw new IllegalArgumentException("Cannot compare offsets with different classes");
        }
        LogOffsetImpl offsetImpl = (LogOffsetImpl) o;
        if (partition.equals(offsetImpl.partition)) {
            throw new IllegalArgumentException("Cannot compare offsets from different partitions");
        }
        return Long.compare(offset, offsetImpl.offset);
    }
}
