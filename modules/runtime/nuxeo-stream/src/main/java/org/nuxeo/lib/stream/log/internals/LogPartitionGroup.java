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

import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.Name;

/**
 * @since 9.3
 */
public class LogPartitionGroup {
    public final Name group;

    public final Name name;

    public final int partition;

    public LogPartitionGroup(Name group, LogPartition mqp) {
        this.group = group;
        this.name = mqp.name();
        this.partition = mqp.partition();
    }

    public LogPartitionGroup(Name group, Name name, int partition) {
        this.group = group;
        this.name = name;
        this.partition = partition;
    }

    public LogPartition getLogPartition() {
        return LogPartition.of(name, partition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LogPartitionGroup that = (LogPartitionGroup) o;
        return partition == that.partition && (group != null ? group.equals(that.group) : that.group == null)
                && (name != null ? name.equals(that.name) : that.name == null);
    }

    @Override
    public int hashCode() {
        int result = group != null ? group.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + partition;
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s:%s-%02d", group, name, partition);
    }

}
