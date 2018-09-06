/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor for a {@link WorkManager} queue configuration.
 *
 * @since 5.6
 */
@XObject("queue")
public class WorkQueueDescriptor implements Descriptor {

    public static final String ALL_QUEUES = "*";

    public static final int DEFAULT_MAX_THREADS = 4;

    public static final int DEFAULT_CLEAR_COMPLETED_AFTER_SECONDS = 600;

    public static final int DEFAULT_CAPACITY = -1;

    @XNode("@id")
    public String id;

    @XNode("@queueing")
    public Boolean queuing;

    @Override
    public String getId() {
        return id;
    }

    /**
     * Whether queuing of work instances to this queue is enabled for this Nuxeo instance.
     */
    public boolean isQueuingEnabled() {
        return !Boolean.FALSE.equals(queuing);
    }

    @XNode("@processing")
    public Boolean processing;

    /**
     * Whether processing of work instances from this queue is enabled for this Nuxeo instance.
     */
    public boolean isProcessingEnabled() {
        return !Boolean.FALSE.equals(processing);
    }

    @XNode("name")
    public String name;

    @XNode("maxThreads")
    public Integer maxThreads;

    public int getMaxThreads() {
        return maxThreads == null ? DEFAULT_MAX_THREADS : maxThreads.intValue();
    }

    @XNodeList(value = "category", type = HashSet.class, componentType = String.class)
    public Set<String> categories = Collections.emptySet();

    /**
     * When specified, make the blocking queue bounded, so submission will block until space become available. This
     * option can not be used with a priority queue.
     *
     * @since 5.7
     */
    @XNode("capacity")
    public Integer capacity;

    public int getCapacity() {
        return capacity == null ? DEFAULT_CAPACITY : capacity.intValue();
    }

    @Override
    public Descriptor merge(Descriptor o) {
        WorkQueueDescriptor other = (WorkQueueDescriptor) o;
        WorkQueueDescriptor merged = new WorkQueueDescriptor();
        merged.id = id;
        merged.name = other.name != null ? other.name : name;
        merged.queuing = other.queuing != null ? other.queuing : queuing;
        merged.capacity = other.capacity != null ? other.capacity : capacity;
        merged.processing = other.processing != null ? other.processing : processing;
        merged.maxThreads = other.maxThreads != null ? other.maxThreads : maxThreads;
        merged.categories = new HashSet<>(categories);
        merged.categories.addAll(other.categories);
        return merged;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName());
        buf.append("(id=");
        buf.append(id);
        buf.append(" categories=");
        buf.append(categories);
        buf.append(" queuing=");
        buf.append(isQueuingEnabled());
        buf.append(" processing=");
        buf.append(isProcessingEnabled());
        buf.append(" maxThreads=");
        buf.append(getMaxThreads());
        buf.append(" capacity=");
        buf.append(getCapacity());
        buf.append(")");
        return buf.toString();
    }
}
