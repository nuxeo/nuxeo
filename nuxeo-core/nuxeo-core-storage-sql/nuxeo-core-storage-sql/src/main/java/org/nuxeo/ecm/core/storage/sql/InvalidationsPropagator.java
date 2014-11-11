/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    /** used for debugging */
    public final String name;

    public InvalidationsPropagator(String name) {
        queues = new ArrayList<InvalidationsQueue>();
        this.name = name;
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

}
