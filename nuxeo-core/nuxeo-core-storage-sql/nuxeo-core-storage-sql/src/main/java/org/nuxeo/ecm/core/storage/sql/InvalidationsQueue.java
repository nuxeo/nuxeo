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

/**
 * Queue of invalidations.
 * <p>
 * All invalidations added are accumulated (from multiple threads), then
 * returned when asked for.
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
