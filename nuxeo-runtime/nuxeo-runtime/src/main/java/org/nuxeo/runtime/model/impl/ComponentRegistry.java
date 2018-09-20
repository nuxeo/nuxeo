/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.runtime.model.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * This class is synchronized to safely update and access the different maps managed by the registry
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ComponentRegistry {

    private final Logger log = LogManager.getLogger();

    /**
     * All registered components including unresolved ones. You can check the state of a component for getting the
     * unresolved ones.
     */
    protected Map<ComponentName, RegistrationInfo> components;

    /**
     * The list of resolved components. We need to use a linked hash map preserve the resolve order. We don't use a
     * simple list to optimize removal by name (used by unregister operations).
     *
     * @since 9.2
     */
    protected LinkedHashMap<ComponentName, RegistrationInfo> resolved;

    /** Map of aliased name to canonical name. */
    protected Map<ComponentName, ComponentName> aliases;

    /**
     * Maps a component name to a set of component names that are depending on that component. Values are always
     * unaliased.
     */
    protected MappedSet requirements;

    /**
     * Map pending components to the set of unresolved components they are waiting for. Key is always unaliased.
     */
    protected MappedSet pendings;

    /**
     * Map deployment source ids to component names This was previously managed by DefaultRuntimeContext - but is no
     * more usable in the original form. This map is only useful for unregister by location - which is used by some
     * tests. Remove this if the unregister API will be removed.
     *
     * @since 9.2
     */
    protected Map<String, ComponentName> deployedFiles;

    public ComponentRegistry() {
        components = new HashMap<>();
        aliases = new HashMap<>();
        requirements = new MappedSet();
        pendings = new MappedSet();
        resolved = new LinkedHashMap<>();
        deployedFiles = new HashMap<>();
    }

    public ComponentRegistry(ComponentRegistry reg) {
        components = new HashMap<>(reg.components);
        aliases = new HashMap<>(reg.aliases);
        requirements = new MappedSet(reg.requirements);
        pendings = new MappedSet(reg.pendings);
        resolved = new LinkedHashMap<>(reg.resolved);
        deployedFiles = new HashMap<>(reg.deployedFiles);
    }

    public synchronized void destroy() {
        components = null;
        aliases = null;
        requirements = null;
        pendings = null;
        deployedFiles = null;
    }

    public synchronized final boolean isResolved(ComponentName name) {
        RegistrationInfo ri = components.get(unaliased(name));
        if (ri == null) {
            return false;
        }
        return ri.getState() > RegistrationInfo.REGISTERED;
    }

    /**
     * @return true if the component was resolved, false if the component is pending
     */
    public synchronized boolean addComponent(RegistrationInfo ri) {
        ComponentName name = ri.getName();
        Set<ComponentName> al = ri.getAliases();
        log.trace("Registering component: {}{}", () -> name, () -> al.isEmpty() ? "" : ", aliases=" + al);
        if (ri.useFormerLifecycleManagement()) {
            ((RegistrationInfoImpl) ri).register();
        } else {
            ri.setState(RegistrationInfo.REGISTERED);
        }
        // map the source id with the component name - see ComponentManager.unregisterByLocation
        String sourceId = ri.getSourceId();
        if (sourceId != null) {
            deployedFiles.put(sourceId, ri.getName());
        }
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

    public synchronized RegistrationInfo removeComponent(ComponentName name) {
        RegistrationInfo ri = components.remove(name);
        if (ri != null) {
            try {
                unresolveComponent(ri);
            } finally {
                if (ri.useFormerLifecycleManagement()) {
                    ((RegistrationInfoImpl) ri).unregister();
                } else {
                    ri.setState(RegistrationInfo.UNREGISTERED);
                }
            }
        }
        return ri;
    }

    /**
     * @return an unmodifiable collection of resolved registration infos, sorted by {@link LinkedHashMap}
     * @since 9.2
     */
    public synchronized Collection<RegistrationInfo> getResolvedRegistrationInfo() {
        return Collections.unmodifiableCollection(resolved.values());
    }

    /**
     * @return an unmodifiable collection of resolved component names, sorted by {@link LinkedHashMap}
     * @since 9.2
     */
    public synchronized Collection<ComponentName> getResolvedNames() {
        return Collections.unmodifiableCollection(resolved.keySet());
    }

    /**
     * @return an unmodifiable collection of missing dependencies
     * @since 9.2
     */
    public synchronized Set<ComponentName> getMissingDependencies(ComponentName name) {
        return Collections.unmodifiableSet(pendings.get(name));
    }

    /**
     * Get the registration info for the given component name or null if none was registered.
     *
     * @since 9.2
     */
    public synchronized RegistrationInfo getComponent(ComponentName name) {
        return components.get(unaliased(name));
    }

    /**
     * Check if the component is already registered against this registry
     */
    public synchronized boolean contains(ComponentName name) {
        return components.containsKey(unaliased(name));
    }

    /**
     * Get the registered components count
     */
    public synchronized int size() {
        return components.size();
    }

    /**
     * @return an unmodifiable collection of registered components
     */
    public synchronized Collection<RegistrationInfo> getComponents() {
        return Collections.unmodifiableCollection(components.values());
    }

    /**
     * Get a copy of the registered components as an array.
     */
    public synchronized RegistrationInfo[] getComponentsArray() {
        return components.values().toArray(new RegistrationInfo[0]);
    }

    /**
     * @return an unmodifiable map of pending components
     */
    public synchronized Map<ComponentName, Set<ComponentName>> getPendingComponents() {
        return Collections.unmodifiableMap(pendings.map);
    }

    protected ComponentName unaliased(ComponentName name) {
        ComponentName alias = aliases.get(name);
        return alias == null ? name : alias;
    }

    /**
     * Fill the pending map with all unresolved dependencies of the given component. Returns false if no unresolved
     * dependencies are found, otherwise returns true.
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

    protected void resolveComponent(RegistrationInfo ri) {
        ComponentName riName = ri.getName();
        Set<ComponentName> names = new HashSet<>();
        names.add(riName);
        names.addAll(ri.getAliases());

        if (ri.useFormerLifecycleManagement()) {
            ((RegistrationInfoImpl) ri).resolve();
        } else {
            ri.setState(RegistrationInfo.RESOLVED);
        }
        resolved.put(ri.getName(), ri); // track resolved components

        // try to resolve pending components that are waiting the newly resolved component
        Set<ComponentName> dependsOnMe = new HashSet<>();
        for (ComponentName n : names) {
            Set<ComponentName> reqs = requirements.get(n);
            if (reqs != null) {
                dependsOnMe.addAll(reqs); // unaliased
            }
        }
        if (dependsOnMe.isEmpty()) {
            return;
        }
        for (ComponentName name : dependsOnMe) { // unaliased
            for (ComponentName n : names) {
                pendings.remove(name, n);
            }
            Set<ComponentName> set = pendings.get(name);
            if (set == null || set.isEmpty()) {
                RegistrationInfo waitingRi = components.get(name);
                resolveComponent(waitingRi);
            }
        }
    }

    protected void unresolveComponent(RegistrationInfo ri) {
        Set<ComponentName> reqs = ri.getRequiredComponents();
        ComponentName name = ri.getName();
        if (ri.useFormerLifecycleManagement()) {
            ((RegistrationInfoImpl) ri).unresolve();
        } else {
            ri.setState(RegistrationInfo.REGISTERED);
        }
        resolved.remove(name);
        pendings.remove(name);
        if (reqs != null) {
            for (ComponentName req : reqs) {
                requirements.remove(req, name);
            }
        }
        Set<ComponentName> set = requirements.get(name); // unaliased
        if (set != null && !set.isEmpty()) {
            for (ComponentName dep : set.toArray(new ComponentName[0])) {
                RegistrationInfo depRi = components.get(dep);
                if (depRi != null) {
                    unresolveComponent(depRi);
                }
            }
        }
    }

    protected static class MappedSet {

        protected Map<ComponentName, Set<ComponentName>> map;

        public MappedSet() {
            map = new HashMap<>();
        }

        /**
         * Create a clone of a mapped set (set values are cloned too)
         */
        public MappedSet(MappedSet mset) {
            this();
            for (Map.Entry<ComponentName, Set<ComponentName>> entry : mset.map.entrySet()) {
                ComponentName name = entry.getKey();
                Set<ComponentName> set = entry.getValue();
                Set<ComponentName> newSet = new HashSet<>(set);
                map.put(name, newSet);
            }
        }

        public Set<ComponentName> get(ComponentName name) {
            return map.get(name);
        }

        public Set<ComponentName> put(ComponentName key, ComponentName value) {
            Set<ComponentName> set = map.computeIfAbsent(key, k -> new HashSet<>());
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
