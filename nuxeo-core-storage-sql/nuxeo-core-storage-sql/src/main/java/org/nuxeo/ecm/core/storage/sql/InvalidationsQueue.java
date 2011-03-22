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
