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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Propagator of invalidations to a set of {@link InvalidationsQueue}s.
 */
public class InvalidationsPropagator {

    public final ArrayList<InvalidationsQueue> queues; // used synchronized

    public InvalidationsPropagator() {
        queues = new ArrayList<InvalidationsQueue>();
    }

    public synchronized void addQueue(InvalidationsQueue queue) {
        if (!queues.contains(queue)) {
            queues.add(queue);
        }
    }

    public synchronized void removeQueue(InvalidationsQueue queue) {
        queues.remove(queue);
    }

    @SuppressWarnings("unchecked")
    public void propagateInvalidations(Invalidations invalidations, InvalidationsQueue skipQueue) {
        Collection<InvalidationsQueue> qq;
        synchronized (this) {
            qq = (Collection<InvalidationsQueue>) queues.clone();
        }
        for (InvalidationsQueue q : qq) {
            if (q != skipQueue) {
                q.addInvalidations(invalidations);
            }
        }
    }

}
