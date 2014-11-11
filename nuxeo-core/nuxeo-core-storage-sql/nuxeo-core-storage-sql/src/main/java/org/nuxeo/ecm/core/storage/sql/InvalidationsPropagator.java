/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
    public void propagateInvalidations(Invalidations invalidations,
            InvalidationsQueue skipQueue) {
        Collection<InvalidationsQueue> qq;
        synchronized (this) {
            qq = (Collection<InvalidationsQueue>) queues.clone();
        }
        int n = 0;
        for (InvalidationsQueue q : qq) {
            if (q != skipQueue) {
                q.addInvalidations(invalidations);
                n++;
            }
        }
    }

}
