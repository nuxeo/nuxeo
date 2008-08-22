/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.contribution.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.contribution.Contribution;
import org.nuxeo.runtime.contribution.ContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractRegistry<K, T> implements
        ContributionRegistry<K, T> {

    protected Map<Object, Contribution<K, T>> registry = new HashMap<Object, Contribution<K, T>>();

    protected Map<Object, List<Contribution<K, T>>> pending = new HashMap<Object, List<Contribution<K, T>>>();

    public synchronized Contribution<K, T> get(K primaryKey) {
        return registry.get(primaryKey);
    }

    public synchronized void remove(K key) {
        Contribution<K, T> contrib = registry.get(key);
        if (contrib != null) {
            contrib.unregister();
        }
        // TODO if all dependents are unregistered remove contribution from
        // registry
    }

    public void removeFragment(K key, T fragment) {
        Contribution<K, T> contrib = registry.get(key);
        if (contrib != null) {
            contrib.removeFragment(fragment);
        }
    }

    public synchronized Contribution<K, T> register(K key, T fragment,
            K... superKeys) {
        Contribution<K, T> contrib = registry.get(key);
        if (contrib == null) {
            contrib = new ContributionImpl<K, T>(this, key);
            registry.put(key, contrib);
        }
        contrib.addFragment(fragment, superKeys);
        return contrib;
    }

    public Contribution<K, T> getOrCreateContribution(K key) {
        Contribution<K, T> contrib = registry.get(key);
        if (contrib == null) {
            contrib = new ContributionImpl<K, T>(this, key);
            registry.put(key, contrib);
            // do not register so that this contribution will be a phantom
        }
        return contrib;
    }

    @SuppressWarnings("unchecked")
    public synchronized void clear() {
        Contribution<K, T>[] contribs = registry.values().toArray(
                new Contribution[registry.size()]);
        for (Contribution<K, T> c : contribs) {
            fireUnresolved(c);
        }
        registry.clear();
    }

    public void fireUnresolved(Contribution<K, T> contrib) {
        uninstall(contrib.getId());
    }

    public void fireResolved(Contribution<K, T> contrib) {
        T value = contrib.merge();
        if (value == null) {
            throw new IllegalStateException("contribution is null");
        }
        install(contrib.getId(), value);
    }

    public void fireUpdated(Contribution<K, T> contrib) {
        T value = contrib.merge();
        if (value == null) {
            throw new IllegalStateException("contribution is null");
        }
        reinstall(contrib.getId(), value);
    }

    protected abstract void applySuperObject(T object, T superObject);

    protected abstract void applyFragment(T object, T fragment);

    protected abstract void install(K key, T object);

    protected abstract void uninstall(K key);

    protected abstract void reinstall(K key, T object);

}
