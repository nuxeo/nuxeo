/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.lock;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.model.LockManager;

/**
 * Descriptor of a {@link LockManager} for the {@link LockManagerService}.
 */
@XObject(value = "lockmanager")
public class LockManagerDescriptor {

    public LockManagerDescriptor() {
    }

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
        return getClass().getSimpleName() + '(' + name + ',' + klass.getName() + ')';
    }

}
