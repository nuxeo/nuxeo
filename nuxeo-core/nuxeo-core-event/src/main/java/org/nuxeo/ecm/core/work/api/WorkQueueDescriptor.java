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

    @XNode("@id")
    public String id;

    @XNode("name")
    public String name;

    @XNode("maxThreads")
    public int maxThreads;

    /**
     * If this is {@code true}, then a priority queue is used instead of a
     * regular queue. In this case, the {@link Work} instances in the queue must
     * implement {@link Comparable} and are prioritized according to their
     * {@code compareTo()} method.
     *
     * @since 5.7
     */
    @XNode("usePriority")
    public boolean usePriority;

    @XNode("clearCompletedAfterSeconds")
    public int clearCompletedAfterSeconds = 3600;

    @XNodeList(value = "category", type = HashSet.class, componentType = String.class)
    public Set<String> categories;

    /**
     * When specified, make the blocking queue bounded, so submission will
     * block until space become available. This option can not be used with
     * a priority queue.
     *
     * @since 5.7
     */
    @XNode("capacity")
    public int capacity = -1;

    @Override
    public WorkQueueDescriptor clone() {
        WorkQueueDescriptor o = new WorkQueueDescriptor();
        o.id = id;
        o.name = name;
        o.maxThreads = maxThreads;
        o.usePriority = usePriority;
        o.clearCompletedAfterSeconds = clearCompletedAfterSeconds;
        o.capacity = capacity;
        o.categories = new HashSet<String>(categories);
        return o;
    }

    public void merge(WorkQueueDescriptor other) {
        name = other.name;
        maxThreads = other.maxThreads;
        usePriority = other.usePriority;
        clearCompletedAfterSeconds = other.clearCompletedAfterSeconds;
        capacity = other.capacity;
        categories.addAll(other.categories);
    }

}
