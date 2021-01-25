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

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.core.api.lock.LockManager;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service holding the registered lock managers.
 * <p>
 * Actual instantiation is done by storage backends.
 *
 * @since 6.0
 */
public class LockManagerService extends DefaultComponent {

    private static final String XP_LOCKMANAGER = "lockmanager";

    protected Map<String, LockManager> lockManagers;

    /**
     * Should start before repository manager.
     */
    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.REPOSITORY - 1;
    }

    @Override
    public void start(ComponentContext context) {
        lockManagers = new ConcurrentHashMap<>();
        this.<LockManagerDescriptor> getRegistryContributions(XP_LOCKMANAGER).forEach(desc -> {
            if (desc.klass != null) {
                try {
                    Constructor<? extends LockManager> ctor = desc.klass.getConstructor(String.class);
                    lockManagers.put(desc.name, ctor.newInstance(name));
                } catch (ReflectiveOperationException e) {
                    addRuntimeMessage(Level.ERROR, e.getMessage());
                    throw new RuntimeServiceException(e);
                }
            }
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        lockManagers = null;
    }

    /**
     * Returns the lock manager registered with the given name.
     *
     * @param name the lock manager name
     * @return the lock manager, or {@code null} if none is registered
     * @since 6.0
     */
    public LockManager getLockManager(String name) {
        return lockManagers.get(name);
    }

    // used by unit tests
    public void registerLockManager(String name, LockManager lockManager) {
        lockManagers.put(name, lockManager);
    }

    /**
     * @since 7.4
     */
    public void unregisterLockManager(String name) {
        lockManagers.remove(name);
    }

}
