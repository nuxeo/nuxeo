/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.common.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// TODO handle dependencies cycles.
@SuppressWarnings({"ClassWithoutToString"})
public class DependencyTree<K, T> implements Iterable<DependencyTree.Entry<K, T>> {

    private final Map<K, Entry<K, T>> registry;

    // the sorted list of resolved entries.
    // given an element e from that list it is ensured that any element at the left
    // of 'e' doesn't depends on it
    private final List<Entry<K, T>> resolved;

    private EventHandler<T> eventHandler;

    public DependencyTree() {
        registry = new HashMap<K, Entry<K, T>>();
        resolved = new ArrayList<Entry<K, T>>();
    }

    public Iterator<Entry<K, T>> iterator() {
        return registry.values().iterator();
    }

    public void add(K key, T object, K ... requires) {
        add(key, object, Arrays.asList(requires));
    }

    public void add(K key, T object, Collection<K> requires) {
        Entry<K, T> entry = registry.get(key);
        if (entry == null) {
            entry = new Entry<K, T>(key, object);
            registry.put(key, entry);
        } else if (entry.object == null) {
            entry.object = object;
        } else {
            //TODO object already exists
            return;
        }
        updateDependencies(entry, requires);
        notifyRegistered(entry);
        // resolve it if no pending requirements
        if (entry.isResolved()) {
            resolve(entry);
        }
    }

    public void add(K key, T object) {
        add(key, object, (Collection<K>) null);
    }

    public void remove(K key) {
        Entry<K, T> entry = registry.remove(key);
        if (entry != null) {
            unregister(entry);
        }
    }

    public void unregister(Entry <K, T> entry) {
        if (entry.isResolved()) {
            unresolve(entry);
        }
        notifyUnregistered(entry);
    }

    public Entry<K, T> getEntry(K key) {
        return registry.get(key);
    }

    public T get(K key) {
        Entry<K, T> entry = registry.get(key);
        return entry != null ? entry.object : null;
    }

    public void resolve(Entry<K, T> entry) {
        resolved.add(entry);
        // notify listener
        notifyResolved(entry);
        // resolve any dependent entry if they are waiting only for me
        Set<Entry<K, T>> deps = entry.getDependsOnMe();
        if (deps != null) {
            for (Entry<K, T> dep : deps) {
                dep.removeWaitingFor(entry);
                if (dep.isResolved()) {
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
                if (!dep.isResolved()) {
                    unresolve(dep); // unresolve the dependent entry
                }
            }
        }
        resolved.remove(entry);
        notifyUnresolved(entry);
    }

    public boolean isRegistered(K key) {
        Entry<K, T> entry = registry.get(key);
        return entry != null && entry.object != null;
    }

    public boolean isResolved(K key) {
        Entry<K, T> entry = registry.get(key);
        return entry != null && entry.isResolved();
    }

    public void setEventHandler(EventHandler<T> eventHandler) {
        this.eventHandler = eventHandler;
    }

    public Collection<Entry<K, T>> getEntries() {
        return registry.values();
    }

    public List<Entry<K, T>> getPendingEntries() {
        List<Entry<K, T>> result = new ArrayList<Entry<K, T>>();
        for (Map.Entry<K, Entry<K, T>> entry : registry.entrySet()) {
            Entry<K, T> val = entry.getValue();
            if (!val.isResolved()) {
                result.add(val);
            }
        }
        return result;
    }

    public List<T> getPendingObjects() {
        List<T> list = new ArrayList<T>();
        List<Entry<K, T>> entries = getPendingEntries();
        for (Entry<K, T> entry : entries) {
            list.add(entry.object);
        }
        return list;
    }

    public List<Entry<K, T>> getMissingRequirements() {
        List<Entry<K, T>> result = new ArrayList<Entry<K, T>>();
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
        List<T> list = new ArrayList<T>();
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
                notifyUnresolved(entry);
                notifyUnregistered(entry);
            }
        }
        resolved.clear();
        Iterator<Entry<K, T>> it = registry.values().iterator();
        while (it.hasNext()) {
            Entry<K, T> entry = it.next();
            it.remove();
            if (entry.isRegistered()) {
                notifyUnregistered(entry);
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
                    reqEntry = new Entry<K, T>(req, null);
                    registry.put(req, reqEntry);
                }
                // dependencies not satisfied
                reqEntry.addDependsOnMe(entry);
                entry.addWaitingFor(reqEntry);
            }
        }
    }


    private void notifyRegistered(Entry<K, T> entry) {
        if (eventHandler != null) {
            eventHandler.registered(entry.object);
        }
    }

    private void notifyUnregistered(Entry<K, T> entry) {
        if (eventHandler != null) {
            eventHandler.unregistered(entry.object);
        }
    }

    private void notifyResolved(Entry<K, T> entry) {
        if (eventHandler != null) {
            eventHandler.resolved(entry.object);
        }
    }

    private void notifyUnresolved(Entry<K, T> entry) {
        if (eventHandler != null) {
            eventHandler.unresolved(entry.object);
        }
    }

    public static class Entry<K, T> {
        private final K key;
        private T object;
        private Set<Entry<K, T>> waitsFor;
        private Set<Entry<K, T>> dependsOnMe;

        public Entry(K key, T object) {
            this.key = key;
            this.object = object;
        }

        public final boolean isRegistered() {
            return object != null;
        }

        public final boolean isResolved() {
            return object != null && waitsFor == null;
        }

        public final void addWaitingFor(Entry<K, T> entry) {
            if (waitsFor == null) {
                waitsFor = new HashSet<Entry<K, T>>();
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
                dependsOnMe = new HashSet<Entry<K, T>>();
            }
            dependsOnMe.add(entry);
        }

        public Set<Entry<K, T>>getDependsOnMe() {
            return dependsOnMe;
        }

        /**
         * @return Returns the waitsFor.
         */
        public Set<Entry<K, T>> getWaitsFor() {
            return waitsFor;
        }

        public final T get() {
            return object;
        }

        /**
         * @return Returns the key.
         */
        public K getKey() {
            return key;
        }

        @Override
        public String toString() {
            return key.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Entry)) {
                return false;
            }
            return key.equals(((Entry) obj).key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    interface EventHandler<T> {

        void registered(T object);

        void unregistered(T object);

        void resolved(T object);

        void unresolved(T object);

    }

}
