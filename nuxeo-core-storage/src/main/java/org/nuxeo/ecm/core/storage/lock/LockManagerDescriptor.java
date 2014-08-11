/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor of a {@link LockManager} for the {@link LockManagerService}.
 */
@XObject(value = "lockmanager")
public class LockManagerDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@class")
    public Class<? extends LockManager> klass;

    /** Copy constructor. */
    public LockManagerDescriptor(LockManagerDescriptor other) {
        name = other.name;
        klass = other.klass;
    }

    public void merge(LockManagerDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.klass != null) {
            klass = other.klass;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ',' + klass.getName()
                + ')';
    }

}
