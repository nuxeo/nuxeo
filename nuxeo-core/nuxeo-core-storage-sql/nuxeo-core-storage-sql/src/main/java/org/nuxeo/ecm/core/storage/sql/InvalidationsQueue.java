/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql;

/**
 * Queue of invalidations.
 * <p>
 * All invalidations added are accumulated (from multiple threads), then returned when asked for.
 */
public class InvalidationsQueue {

    public Invalidations queue; // used under synchronization

    /** used for debugging */
    public final String name;

    public InvalidationsQueue(String name) {
        queue = new Invalidations();
        this.name = name;
    }

    /**
     * Adds invalidations.
     * <p>
     * May be called asynchronously from multiple threads.
     */
    public synchronized void addInvalidations(Invalidations invalidations) {
        queue.add(invalidations);
    }

    /**
     * Gets the queued invalidations and resets the queue.
     */
    public synchronized Invalidations getInvalidations() {
        Invalidations invalidations = queue;
        queue = new Invalidations();
        return invalidations;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

}
