/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.lock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Service holding the registered lock managers.
 * <p>
 * Actual instantiation is done by storage backends.
 *
 * @since 6.0
 */
public class LockManagerService extends DefaultComponent {

    private static final Log log = LogFactory.getLog(LockManagerService.class);

    private static final String XP_LOCKMANAGER = "lockmanager";

    protected LockManagerDescriptorRegistry registry = new LockManagerDescriptorRegistry();

    protected Map<String, LockManager> lockManagers = new ConcurrentHashMap<>();

    protected static class LockManagerDescriptorRegistry extends
            SimpleContributionRegistry<LockManagerDescriptor> {

        @Override
        public String getContributionId(LockManagerDescriptor contrib) {
            return contrib.name;
        }

        @Override
        public LockManagerDescriptor clone(LockManagerDescriptor orig) {
            return new LockManagerDescriptor(orig);
        }

        @Override
        public void merge(LockManagerDescriptor src, LockManagerDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        public void clear() {
            currentContribs.clear();
        }

        public LockManagerDescriptor getLockManagerDescriptor(String id) {
            return getCurrentContribution(id);
        }
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        registry.clear();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        registry.clear();
    }

    @Override
    public void registerContribution(Object contrib, String xpoint,
            ComponentInstance contributor) {
        if (XP_LOCKMANAGER.equals(xpoint)) {
            addContribution((LockManagerDescriptor) contrib);
        } else {
            throw new NuxeoException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint,
            ComponentInstance contributor) throws Exception {
        if (XP_LOCKMANAGER.equals(xpoint)) {
            removeContribution((LockManagerDescriptor) contrib);
        } else {
            throw new NuxeoException("Unknown extension point: " + xpoint);
        }
    }

    protected void addContribution(LockManagerDescriptor descriptor) {
        log.info("Registered " + descriptor);
        registry.addContribution(descriptor);
    }

    protected void removeContribution(LockManagerDescriptor descriptor) {
        log.info("Unregistered " + descriptor);
        registry.removeContribution(descriptor);
    }

    /**
     * Returns the lock manager registered with the given name.
     * <p>
     * Lazily constructs it if needed.
     *
     * @param name the lock manager name
     * @return the lock manager, or {@code null} if none is registered
     * @since 6.0
     */
    public synchronized LockManager getLockManager(String name) {
        LockManager lockManager = lockManagers.get(name);
        if (lockManager == null) {
            LockManagerDescriptor descriptor = registry.getLockManagerDescriptor(name);
            if (descriptor == null) {
                return null;
            }
            try {
                Constructor<? extends LockManager> ctor = descriptor.klass.getConstructor(String.class);
                lockManager = ctor.newInstance(name);
            } catch (NoSuchMethodException | InstantiationException
                    | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                throw new NuxeoException(e);
            }
            registerLockManager(name, lockManager);
        }
        return lockManager;
    }

    // used by unit tests
    public void registerLockManager(String name, LockManager lockManager) {
        lockManagers.put(name, lockManager);
    }

}
