/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> TODO this was copied from nuxeo.commons and fixed -
 *         should put it back with all modifs
 */
// TODO handle dependencies cycles.
public class DependencyTree<K, T> implements Iterable<DependencyTree.Entry<K, T>> {

    private final Map<K, Entry<K, T>> registry;

    // the sorted list of resolved entries.
    // given an element e from that list it is ensured that any element at the left
    // of 'e' doesn't depends on it
    private final List<Entry<K, T>> resolved;

    public DependencyTree() {
        registry = new Hashtable<>();
        resolved = new Vector<>();
    }

    @Override
    public Iterator<Entry<K, T>> iterator() {
        return registry.values().iterator();
    }

    public Entry<K, T> add(K key, T object, @SuppressWarnings("unchecked") K... requires) {
        return add(key, object, Arrays.asList(requires));
    }

    public Entry<K, T> add(K key, T object, Collection<K> requires) {
        Entry<K, T> entry = registry.get(key);
        if (entry == null) {
            entry = new Entry<>(key, object);
            registry.put(key, entry);
        } else if (entry.object == null) {
            entry.object = object;
        } else {
            // TODO object already exists
            return entry;
        }
        updateDependencies(entry, requires);
        registered(entry);
        // resolve it if no pending requirements
        if (entry.canEnterResolvedState()) {
            resolve(entry);
        }
        return entry;
    }

    public void add(K key, T object) {
        add(key, object, (Collection<K>) null);
    }

    public void remove(K key) {
        Entry<K, T> entry = registry.get(key); // do not remove entry
        if (entry != null) {
            unregister(entry);
            entry.object = null;
            entry.waitsFor = null;
        }
    }

    public void unregister(Entry<K, T> entry) {
        if (entry.isResolved()) {
            unresolve(entry);
        }
        unregistered(entry);
    }

    public Entry<K, T> getEntry(K key) {
        return registry.get(key);
    }

    public T get(K key) {
        Entry<K, T> entry = registry.get(key);
        return entry != null ? entry.object : null;
    }

    public T getResolved(K key) {
        Entry<K, T> entry = registry.get(key);
        return entry != null && entry.isResolved() ? entry.object : null;
    }

    private void resolveEntry(Entry<K, T> entry) {
        // synchronize () {
        resolved.add(entry);
        entry.isResolved = true;
        // }
        // notify listener
        resolved(entry);
    }

    private void unresolveEntry(Entry<K, T> entry) {
        // synchronize () {
        resolved.remove(entry);
        entry.isResolved = false;
        // }
        unresolved(entry);
    }

    public void resolve(Entry<K, T> entry) {
        resolveEntry(entry);
        // resolve any dependent entry if they are waiting only for me
        Set<Entry<K, T>> deps = entry.getDependsOnMe();
        if (deps != null) {
            for (Entry<K, T> dep : deps) {
                dep.removeWaitingFor(entry);
                if (dep.canEnterResolvedState()) {
                    resolve(dep); // resolve the dependent entry
                }
            }
        }
    }

    public void unresolve(Entry<K, T> entry) {
        // unresolve any dependent object
        Set<Entry<K, T>> deps = entry.getDependsOnMe();
        if (deps != null) {
            for (Entry<K, T> dep : deps) {
                dep.addWaitingFor(entry);
                if (dep.isResolved()) {
                    unresolve(dep); // unresolve the dependent entry
                }
            }
        }
        unresolveEntry(entry);
    }

    public boolean isPhantom(K key) {
        Entry<K, T> entry = registry.get(key);
        return entry != null && entry.isPhantom();
    }

    public boolean isRegistered(K key) {
        Entry<K, T> entry = registry.get(key);
        return entry != null && entry.isRegistered();
    }

    public boolean isResolved(K key) {
        Entry<K, T> entry = registry.get(key);
        return entry != null && entry.isResolved();
    }

    public Collection<Entry<K, T>> getEntries() {
        return registry.values();
    }

    public List<T> getRegisteredObjects() {
        List<T> list = new ArrayList<>();
        Collection<Entry<K, T>> entries = getEntries();
        for (Entry<K, T> entry : entries) {
            list.add(entry.object);
        }
        return list;
    }

    public List<Entry<K, T>> getPendingEntries() {
        List<Entry<K, T>> result = new ArrayList<>();
        for (Map.Entry<K, Entry<K, T>> entry : registry.entrySet()) {
            Entry<K, T> val = entry.getValue();
            if (!val.isResolved()) {
                result.add(val);
            }
        }
        return result;
    }

    public List<T> getPendingObjects() {
        List<T> list = new ArrayList<>();
        List<Entry<K, T>> entries = getPendingEntries();
        for (Entry<K, T> entry : entries) {
            list.add(entry.object);
        }
        return list;
    }

    public List<Entry<K, T>> getMissingRequirements() {
        List<Entry<K, T>> result = new ArrayList<>();
        for (Map.Entry<K, Entry<K, T>> entry : registry.entrySet()) {
            Entry<K, T> val = entry.getValue();
            if (!val.isRegistered()) {
                result.add(val);
            }
        }
        return result;
    }

    /**
     * Entries are sorted so an entry never depends on entries on its right.
     */
    public List<Entry<K, T>> getResolvedEntries() {
        return resolved;
    }

    public List<T> getResolvedObjects() {
        List<T> list = new ArrayList<>();
        List<Entry<K, T>> entries = resolved;
        for (Entry<K, T> entry : entries) {
            list.add(entry.object);
        }
        return list;
    }

    public void clear() {
        for (Entry<K, T> entry : resolved) {
            entry = registry.remove(entry.key);
            if (entry != null) {
                entry.isResolved = false;
                unresolved(entry);
                unregistered(entry);
            }
        }
        resolved.clear();
        Iterator<Entry<K, T>> it = registry.values().iterator();
        while (it.hasNext()) {
            Entry<K, T> entry = it.next();
            it.remove();
            if (entry.isRegistered()) {
                unregistered(entry);
            }
        }
    }

    protected void updateDependencies(Entry<K, T> entry, Collection<K> requires) {
        if (requires != null) {
            for (K req : requires) {
                Entry<K, T> reqEntry = registry.get(req);
                if (reqEntry != null) {
                    if (reqEntry.isResolved()) {
                        // requirement satisfied -> continue
                        reqEntry.addDependsOnMe(entry);
                        continue;
                    }
                } else {
                    reqEntry = new Entry<>(req, null); // placeholder entry
                    registry.put(req, reqEntry);
                }
                // dependencies not satisfied
                reqEntry.addDependsOnMe(entry);
                entry.addWaitingFor(reqEntry);
            }
        }
    }

    protected void registered(Entry<K, T> entry) {
    }

    protected void unregistered(Entry<K, T> entry) {
    }

    protected void resolved(Entry<K, T> entry) {
    }

    protected void unresolved(Entry<K, T> entry) {
    }

    public static final int PHANTOM = 0;

    public static final int REGISTERED = 1;

    public static final int RESOLVED = 3;

    public static class Entry<K, T> {
        private final K key;

        private T object;

        private Set<Entry<K, T>> waitsFor;

        private Set<Entry<K, T>> dependsOnMe;

        private boolean isResolved = false;

        public Entry(K key, T object) {
            this.key = key;
            this.object = object;
        }

        public boolean isPhantom() {
            return object == null;
        }

        public boolean isRegistered() {
            return object != null;
        }

        public boolean isResolved() {
            return isResolved;
        }

        public final boolean canEnterResolvedState() {
            return !isResolved && object != null && waitsFor == null;
        }

        public final void addWaitingFor(Entry<K, T> entry) {
            if (waitsFor == null) {
                waitsFor = new HashSet<>();
            }
            waitsFor.add(entry);
        }

        public final void removeWaitingFor(Entry<K, T> key) {
            if (waitsFor != null) {
                waitsFor.remove(key);
                if (waitsFor.isEmpty()) {
                    waitsFor = null;
                }
            }
        }

        public final void addDependsOnMe(Entry<K, T> entry) {
            if (dependsOnMe == null) {
                dependsOnMe = new HashSet<>();
            }
            dependsOnMe.add(entry);
        }

        public Set<Entry<K, T>> getDependsOnMe() {
            return dependsOnMe;
        }

        public Set<Entry<K, T>> getWaitsFor() {
            return waitsFor;
        }

        public final T get() {
            return object;
        }

        public K getKey() {
            return key;
        }

        @Override
        public String toString() {
            return key.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Entry)) {
                return false;
            }
            return key.equals(((Entry<?, ?>) obj).key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

}
