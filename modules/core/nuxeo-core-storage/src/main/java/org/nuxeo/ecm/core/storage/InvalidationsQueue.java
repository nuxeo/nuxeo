/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage;

import org.nuxeo.runtime.pubsub.SerializableAccumulableInvalidations;

/**
 * Queue of invalidations.
 * <p>
 * All invalidations added are accumulated (from multiple threads), then returned when asked for.
 *
 * @param <T> the invalidations type
 * @since 11.1
 */
public abstract class InvalidationsQueue<T extends SerializableAccumulableInvalidations> {

    public T queue; // used under synchronization

    /** used for debugging */
    public final String name;

    public InvalidationsQueue(String name) {
        queue = newInvalidations();
        this.name = name;
    }

    /** Constructs new empty invalidations, of type {@link T}. */
    public abstract T newInvalidations();

    /**
     * Adds invalidations.
     * <p>
     * May be called asynchronously from multiple threads.
     */
    public synchronized void addInvalidations(T invalidations) {
        queue.add(invalidations);
    }

    /**
     * Gets the queued invalidations and resets the queue.
     */
    public synchronized T getInvalidations() {
        T invalidations = queue;
        queue = newInvalidations();
        return invalidations;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

}
