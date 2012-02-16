/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
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

    /** Map of aliased name to canonical name. */
    protected Map<ComponentName, ComponentName> aliases;

    /**
     * Maps a component name to a set of component names that are depending on
     * that component. Values are always unaliased.
     */
    protected MappedSet requirements;

    /**
     * Map pending components to the set of unresolved components they are
     * waiting for. Key is always unaliased.
     */
    protected MappedSet pendings;

    public ComponentRegistry() {
        components = new HashMap<ComponentName, RegistrationInfoImpl>();
        aliases = new HashMap<ComponentName, ComponentName>();
        requirements = new MappedSet();
        pendings = new MappedSet();
    }

    public void destroy() {
        components = null;
        aliases = null;
        requirements = null;
        pendings = null;
    }

    protected ComponentName unaliased(ComponentName name) {
        ComponentName alias = aliases.get(name);
        return alias == null ? name : alias;
    }

    public final boolean isResolved(ComponentName name) {
        RegistrationInfo ri = components.get(unaliased(name));
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
        ComponentName name = ri.getName();
        Set<ComponentName> al = ri.getAliases();
        String aliasInfo = al.isEmpty() ? "" : ", aliases=" + al;
        log.info("Registering component: " + name + aliasInfo);
        ri.register();
        components.put(name, ri);
        for (ComponentName n : al) {
            aliases.put(n, name);
        }
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
        return components.get(unaliased(name));
    }

    public boolean contains(ComponentName name) {
        return components.containsKey(unaliased(name));
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
        ComponentName riName = ri.getName();
        Set<ComponentName> names = new HashSet<ComponentName>();
        names.add(riName);
        names.addAll(ri.getAliases());

        ri.resolve();
        // try to resolve pending components that are waiting the newly
        // resolved component
        Set<ComponentName> dependsOnMe = new HashSet<ComponentName>();
        for (ComponentName n : names) {
            Set<ComponentName> reqs = requirements.get(n);
            if (reqs != null) {
                dependsOnMe.addAll(reqs); // unaliased
            }
        }
        if (dependsOnMe == null || dependsOnMe.isEmpty()) {
            return;
        }
        for (ComponentName name : dependsOnMe) { // unaliased
            for (ComponentName n : names) {
                pendings.remove(name, n);
            }
            Set<ComponentName> set = pendings.get(name);
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
        Set<ComponentName> set = requirements.get(name); // unaliased
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
