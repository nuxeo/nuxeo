/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

/**
 * Descriptor for a {@link WorkManager} queue configuration.
 *
 * @since 5.6
 */
@XObject("queue")
public class WorkQueueDescriptor {

    public static final String ALL_QUEUES = "*";

    public static final int DEFAULT_MAX_THREADS = 4;

    public static final int DEFAULT_CLEAR_COMPLETED_AFTER_SECONDS = 3600;

    public static final int DEFAULT_CAPACITY = -1;

    @XNode("@id")
    public String id;

    @XNode("@queueing")
    public Boolean queuing;

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

    @XNode("clearCompletedAfterSeconds")
    public Integer clearCompletedAfterSeconds;

    public int getClearCompletedAfterSeconds() {
        return clearCompletedAfterSeconds == null ? DEFAULT_CLEAR_COMPLETED_AFTER_SECONDS : clearCompletedAfterSeconds.intValue();
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
    public WorkQueueDescriptor clone() {
        WorkQueueDescriptor o = new WorkQueueDescriptor();
        o.id = id;
        o.queuing = queuing;
        o.processing = processing;
        o.name = name;
        o.maxThreads = maxThreads;
        o.clearCompletedAfterSeconds = clearCompletedAfterSeconds;
        o.capacity = capacity;
        o.categories = new HashSet<String>(categories);
        return o;
    }

    public void merge(WorkQueueDescriptor other) {
        if (other.queuing != null) {
            queuing = other.queuing;
        }
        if (other.processing != null) {
            processing = other.processing;
        }
        if (other.name != null) {
            name = other.name;
        }
        if (other.maxThreads != null) {
            maxThreads = other.maxThreads;
        }
        if (other.clearCompletedAfterSeconds != null) {
            clearCompletedAfterSeconds = other.clearCompletedAfterSeconds;
        }
        if (other.capacity != null) {
            capacity = other.capacity;
        }
        categories.addAll(other.categories);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName());
        buf.append("[id=");
        buf.append(id);
        buf.append(" categories=");
        buf.append(categories);
        if (queuing != null) {
            buf.append(" queuing=");
            buf.append(queuing);
        }
        if (processing != null) {
            buf.append(" processing=");
            buf.append(processing);
        }
        if (maxThreads != null) {
            buf.append(" maxThreads=");
            buf.append(maxThreads);
        }
        if (capacity != null) {
            buf.append(" capacity=");
            buf.append(capacity);
        }
        if (clearCompletedAfterSeconds != null) {
            buf.append(" clearCompletedAfterSeconds=");
            buf.append(clearCompletedAfterSeconds);
        }
        buf.append("]");
        return buf.toString();
    }

    public String toEffectiveString() {
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
        buf.append(" clearCompletedAfterSeconds=");
        buf.append(getClearCompletedAfterSeconds());
        buf.append(")");
        return buf.toString();
    }

}
