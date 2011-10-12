/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.model.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class ComponentRegistry {

    private final Log log = LogFactory.getLog(ComponentRegistry.class);

    /**
     * All registered components including unresolved ones. You can check the
     * state of a component for getting the unresolved ones.
     */
    protected Map<ComponentName, RegistrationInfoImpl> components;

    /**
     * Maps a component name to a set of component names that are depending on
     * that component.
     */
    protected MappedSet requirements;

    /**
     * Map pending components to the set of unresolved components they are
     * waiting for.
     */
    protected MappedSet pendings;

    public ComponentRegistry() {
        components = new HashMap<ComponentName, RegistrationInfoImpl>();
        requirements = new MappedSet();
        pendings = new MappedSet();
    }

    public void destroy() {
        components = null;
        requirements = null;
        pendings = null;
    }

    public final boolean isResolved(ComponentName name) {
        RegistrationInfo ri = components.get(name);
        if (ri == null) {
            return false;
        }
        return ri.getState() > RegistrationInfo.REGISTERED;
    }

    /**
     * Fill the pending map with all unresolved dependencies of the given
     * component. Returns false if no unresolved dependencies are found,
     * otherwise returns true.
     * 
     * @param ri
     * @return
     */
    protected final boolean computePendings(RegistrationInfo ri) {
        Set<ComponentName> set = ri.getRequiredComponents();
        if (set == null || set.isEmpty()) {
            return false;
        }
        boolean hasUnresolvedDependencies = false;
        // fill the requirements and pending map
        for (ComponentName name : set) {
            if (!isResolved(name)) {
                pendings.put(ri.getName(), name);
                hasUnresolvedDependencies = true;
            }
            requirements.put(name, ri.getName());
        }
        return hasUnresolvedDependencies;
    }

    /**
     * 
     * @param ri
     * @return true if the component was resolved, false if the component is
     *         pending
     */
    public boolean addComponent(RegistrationInfoImpl ri) throws Exception {
        log.info("Registering component: " + ri.getName());
        ri.register();
        components.put(ri.getName(), ri);
        boolean hasUnresolvedDependencies = computePendings(ri);
        if (!hasUnresolvedDependencies) {
            resolveComponent(ri);
            return true;
        }
        return false;
    }

    public RegistrationInfoImpl removeComponent(ComponentName name)
            throws Exception {
        RegistrationInfoImpl ri = components.remove(name);
        if (ri != null) {
            try {
                unresolveComponent(ri);
            } finally {
                ri.unregister();
            }
        }
        return ri;
    }

    public Set<ComponentName> getMissingDependencies(ComponentName name) {
        return pendings.get(name);
    }

    public RegistrationInfoImpl getComponent(ComponentName name) {
        return components.get(name);
    }

    public boolean contains(ComponentName name) {
        return components.containsKey(name);
    }

    public int size() {
        return components.size();
    }

    public Collection<RegistrationInfoImpl> getComponents() {
        return components.values();
    }

    public RegistrationInfoImpl[] getComponentsArray() {
        return components.values().toArray(
                new RegistrationInfoImpl[components.size()]);
    }

    public Map<ComponentName, Set<ComponentName>> getPendingComponents() {
        return pendings.map;
    }

    protected void resolveComponent(RegistrationInfoImpl ri) throws Exception {
        ri.resolve();
        // try to resolve pending components that are waiting the newly
        // resolved component
        Set<ComponentName> dependsOnMe = requirements.get(ri.getName());
        if (dependsOnMe == null || dependsOnMe.isEmpty()) {
            return;
        }
        for (ComponentName name : dependsOnMe) {
            Set<ComponentName> set = pendings.remove(name, ri.getName());
            if (set == null || set.isEmpty()) {
                RegistrationInfoImpl waitingRi = components.get(name);
                resolveComponent(waitingRi);
            }
        }
    }

    protected void unresolveComponent(RegistrationInfoImpl ri) throws Exception {
        Set<ComponentName> reqs = ri.getRequiredComponents();
        ComponentName name = ri.getName();
        ri.unresolve();
        pendings.remove(name);
        if (reqs != null) {
            for (ComponentName req : reqs) {
                requirements.remove(req, name);
            }
        }
        Set<ComponentName> set = requirements.get(name);
        if (set != null && !set.isEmpty()) {
            for (ComponentName dep : set.toArray(new ComponentName[set.size()])) {
                RegistrationInfoImpl depRi = components.get(dep);
                if (depRi != null) {
                    unresolveComponent(depRi);
                }
            }
        }
    }

    static class MappedSet {
        protected Map<ComponentName, Set<ComponentName>> map;

        public MappedSet() {
            map = new HashMap<ComponentName, Set<ComponentName>>();
        }

        public Set<ComponentName> get(ComponentName name) {
            return map.get(name);
        }

        public Set<ComponentName> put(ComponentName key, ComponentName value) {
            Set<ComponentName> set = map.get(key);
            if (set == null) {
                set = new HashSet<ComponentName>();
                map.put(key, set);
            }
            set.add(value);
            return set;
        }

        public Set<ComponentName> remove(ComponentName key) {
            return map.remove(key);
        }

        public Set<ComponentName> remove(ComponentName key, ComponentName value) {
            Set<ComponentName> set = map.get(key);
            if (set != null) {
                set.remove(value);
                if (set.isEmpty()) {
                    map.remove(key);
                }
            }
            return set;
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public int size() {
            return map.size();
        }

        public void clear() {
            map.clear();
        }
    }
}
