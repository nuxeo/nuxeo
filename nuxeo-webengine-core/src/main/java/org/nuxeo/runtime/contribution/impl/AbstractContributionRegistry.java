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
import java.util.Map;

import org.nuxeo.runtime.contribution.Contribution;
import org.nuxeo.runtime.contribution.ContributionRegistry;

/**
 * The parent provider is read only. It is never modified by the registry.
 * It serves only to resolve dependencies. This allow greater flexibility in managing dependencies.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractContributionRegistry<K, T> implements
        ContributionRegistry<K, T> {

    protected final Map<Object, Contribution<K, T>> registry;

    protected AbstractContributionRegistry() {
        registry = new HashMap<Object, Contribution<K, T>>();
    }

    public synchronized Contribution<K, T> getContribution(K primaryKey) {
        return registry.get(primaryKey);
    }

    public T getObject(K key) {
        Contribution<K,T> contrib = getContribution(key);
        if (contrib != null) {
            if (contrib.isResolved()) {
                return contrib.getValue();
            }
        }
        return null;
    }

    public synchronized void removeContribution(K key) {
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

    public synchronized Contribution<K, T> addFragment(K key, T fragment,
            K... superKeys) {
        Contribution<K, T> contrib = registry.get(key);
        if (contrib == null) {
            contrib = new ContributionImpl<K, T>(this, key);
            registry.put(key, contrib);
        }
        contrib.addFragment(fragment, superKeys);
        return contrib;
    }

    public synchronized Contribution<K, T> getOrCreateDependency(K key) {
        Contribution<K, T> contrib = registry.get(key);
        if (contrib == null) {
            contrib = new ContributionImpl<K, T>(this, key);
            registry.put(key, contrib);
            // do not register so that this contribution will be a phantom
        }
        return contrib;
    }

    public void fireUnresolved(Contribution<K, T> contrib, T value) {
        uninstallContribution(contrib.getId(), value);
    }

    public void fireResolved(Contribution<K, T> contrib) {
        T value = contrib.getValue();
        if (value == null) {
            throw new IllegalStateException("contribution is null");
        }
        installContribution(contrib.getId(), value);
    }

    public void fireUpdated(T oldValue, Contribution<K, T> contrib) {
        T value = contrib.getValue();
        if (value == null) {
            throw new IllegalStateException("contribution is null");
        }
        updateContribution(contrib.getId(), value, oldValue);
    }


    public void dispose() {
        registry.clear();
    }

    protected abstract T clone(T object);

    /**
     * Applies fragment over the given object.
     *
     * @param object
     * @param fragment
     */
    protected void applyFragment(T object, T fragment) {
        // do nothing
    }

    protected void applySuperFragment(T object, T superFragment) {
        // do nothing
    }

    protected abstract void installContribution(K key, T object);

    protected abstract void uninstallContribution(K key, T object);

    protected boolean isMainFragment(T object) {
        return true;
    }

    protected void updateContribution(K key, T object, T oldValue) {
        uninstallContribution(key, oldValue);
        installContribution(key, object);
    }

}
