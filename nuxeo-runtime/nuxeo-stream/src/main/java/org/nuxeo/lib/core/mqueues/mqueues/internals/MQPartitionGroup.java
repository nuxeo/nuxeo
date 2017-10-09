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

import org.nuxeo.lib.core.mqueues.mqueues.MQPartition;

/**
 * @since 9.2
 */
public class MQPartitionGroup {
    public final String group;
    public final String name;
    public final int partition;

    public MQPartitionGroup(String group, MQPartition mqp) {
        this.group = group;
        this.name = mqp.name();
        this.partition = mqp.partition();
    }


    public MQPartitionGroup(String group, String name, int partition) {
        this.group = group;
        this.name = name;
        this.partition = partition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MQPartitionGroup that = (MQPartitionGroup) o;

        if (partition != that.partition) return false;
        if (group != null ? !group.equals(that.group) : that.group != null) return false;
        return name != null ? name.equals(that.name) : that.name == null;
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