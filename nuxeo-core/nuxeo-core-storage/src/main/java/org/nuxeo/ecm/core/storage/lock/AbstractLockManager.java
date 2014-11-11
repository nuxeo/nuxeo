/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.lock;

import org.nuxeo.ecm.core.api.Lock;

/**
 * Base implementation for lock managers.
 *
 * @since 5.9.6
 */
public abstract class AbstractLockManager implements LockManager {

    @Override
    public boolean canLockBeRemoved(Lock lock, String owner) {
        return canLockBeRemovedStatic(lock, owner);
    }

    // temporary static method before refactoring
    public static boolean canLockBeRemovedStatic(Lock lock, String owner) {
        return owner == null || owner.equals(lock.getOwner());
    }

}
