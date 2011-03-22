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
package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Information about a lock set on a document.
 * <p>
 * The lock information holds the owner, which is a user id, and the lock
 * creation time.
 */
public class Lock implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String owner;

    private final Calendar created;

    private final boolean failed;

    public Lock(String owner, Calendar created) {
        this.owner = owner;
        this.created = created;
        this.failed = false;
    }

    public Lock(Lock lock, boolean failed) {
        this.owner = lock.owner;
        this.created = lock.created;
        this.failed = failed;
    }

    /**
     * The owner of the lock.
     *
     * @return the owner, which is a user id
     */
    public String getOwner() {
        return owner;
    }

    /**
     * The creation time of the lock.
     *
     * @return the creation time
     */
    public Calendar getCreated() {
        return created;
    }

    /**
     * The failure state, used for removal results.
     *
     * @return the failure state
     */
    public boolean getFailed() {
        return failed;
    }


}
