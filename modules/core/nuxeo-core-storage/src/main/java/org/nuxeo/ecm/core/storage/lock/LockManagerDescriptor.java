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
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.core.api.lock.LockManager;

/**
 * Descriptor of a {@link LockManager} for the {@link LockManagerService}.
 */
@XObject(value = "lockmanager")
@XRegistry
public class LockManagerDescriptor {

    @XNode("@name")
    @XRegistryId
    public String name;

    @XNode("@class")
    public Class<? extends LockManager> klass;

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ',' + klass.getName() + ')';
    }

}
