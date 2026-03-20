/*
 * (C) Copyright 2014-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.lock.LockManager;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor of a {@link LockManager} for the {@link LockManagerService}.
 */
@XObject(value = "lockmanager")
public class LockManagerDescriptor implements Descriptor {

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

    @Override
    public String getId() {
        return name;
    }

    @Override
    public LockManagerDescriptor merge(Descriptor o) {
        var other = (LockManagerDescriptor) o;
        var merged = new LockManagerDescriptor(this);
        // we merge based on name, so no name merging needed
        merged.klass = getIfNull(other.klass, klass);
        return merged;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ',' + klass.getName() + ')';
    }
}
