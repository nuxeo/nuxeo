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
package org.nuxeo.lib.core.mqueues.mqueues.internals;

import org.nuxeo.lib.core.mqueues.mqueues.MQOffset;
import org.nuxeo.lib.core.mqueues.mqueues.MQPartition;

/**
 * @since 9.1
 */
public class MQOffsetImpl implements MQOffset {
    protected MQPartition partition;
    protected final long offset;

    public MQOffsetImpl(MQPartition partition, long offset) {
        this.partition = partition;
        this.offset = offset;
    }

    public MQOffsetImpl(String name, int partition, long offset) {
        this.partition = MQPartition.of(name, partition);
        this.offset = offset;
    }

    @Override
    public MQPartition partition() {
        return partition;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public String toString() {
        return String.format("%s:+%d", partition, offset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MQOffsetImpl offsetImpl = (MQOffsetImpl) o;

        if (!partition.equals(offsetImpl.partition)) return false;
        return offset == offsetImpl.offset;
    }

    @Override
    public int hashCode() {
        int result = partition != null ? partition.hashCode() : 0;
        result = 31 * result + (int) (offset ^ (offset >>> 32));
        return result;
    }

    @Override
    public int compareTo(MQOffset o) {
        if (this == o) return 0;
        if (o == null || getClass() != o.getClass()) {
            throw new IllegalArgumentException("Can not compare offsets with different classes");
        }
        MQOffsetImpl offsetImpl = (MQOffsetImpl) o;
        if (partition.equals(offsetImpl.partition)) {
            throw new IllegalArgumentException("Can not compare offsets from different partitions");
        }
        return (int) (offset - offsetImpl.offset);
    }
}
