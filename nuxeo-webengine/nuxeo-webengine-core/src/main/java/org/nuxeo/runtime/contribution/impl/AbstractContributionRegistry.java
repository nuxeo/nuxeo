/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.contribution.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.runtime.contribution.Contribution;
import org.nuxeo.runtime.contribution.ContributionRegistry;

/**
 * The parent provider is read only. It is never modified by the registry. It serves only to resolve dependencies. This
 * allows greater flexibility in managing dependencies. This registry may have a parent registry that can be used only
 * read only.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO need to implement a visibility (PRIVATE, PROTECTED, PUBLIC etc)
// on contributions when extending other registries
public abstract class AbstractContributionRegistry<K, T> implements ContributionRegistry<K, T> {

    protected final Map<Object, Contribution<K, T>> registry;

    protected final AbstractContributionRegistry<K, T> parent;

    protected final List<AbstractContributionRegistry<K, T>> listeners;

    protected AbstractContributionRegistry() {
        this(null);
    }

    protected AbstractContributionRegistry(AbstractContributionRegistry<K, T> parent) {
        registry = new HashMap<Object, Contribution<K, T>>();
        this.parent = parent;
        listeners = new ArrayList<AbstractContributionRegistry<K, T>>();
        // subclasses may call importParentContributions(); after initializing the registry
        // this will import all resolved contributions from the parent
    }

    public ContributionRegistry<K, T> getParent() {
        return parent;
    }

    protected synchronized void importParentContributions() {
        AbstractContributionRegistry<K, T> pParent = parent;
        List<AbstractContributionRegistry<K, T>> parents = new ArrayList<AbstractContributionRegistry<K, T>>();
        while (pParent != null) {
            parents.add(pParent);
            pParent = pParent.parent;
        }
        Collections.reverse(parents);
        for (AbstractContributionRegistry<K, T> p : parents) {
            p.listeners.add(this);
            for (Contribution<K, T> contrib : p.registry.values().toArray(new Contribution[p.registry.size()])) {
                if (contrib.isResolved()) {
                    installContribution(contrib.getId(), contrib.getValue());
                }
            }
            p = p.parent;
        }
    }

    public synchronized Contribution<K, T> getContribution(K primaryKey) {
        Contribution<K, T> contrib = registry.get(primaryKey);
        if (contrib == null && parent != null) {
            contrib = parent.getContribution(primaryKey);
        }
        return contrib;
    }

    public T getObject(K key) {
        Contribution<K, T> contrib = getContribution(key);
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

    public synchronized Contribution<K, T> addFragment(K key, T fragment, K... superKeys) {
        Contribution<K, T> contrib = registry.get(key);
        if (contrib == null) {
            contrib = new ContributionImpl<K, T>(this, key);
            registry.put(key, contrib);
        }
        contrib.addFragment(fragment, superKeys);
        return contrib;
    }

    public synchronized Contribution<K, T> getOrCreateDependency(K key) {
        Contribution<K, T> contrib = getContribution(key);
        if (contrib == null) {
            contrib = new ContributionImpl<K, T>(this, key);
            registry.put(key, contrib);
            // do not register so that this contribution will be a phantom
        }
        return contrib;
    }

    public void fireUnresolved(Contribution<K, T> contrib, T value) {
        K key = contrib.getId();
        uninstallContribution(key, value);
        if (!listeners.isEmpty()) {
            for (AbstractContributionRegistry<K, T> reg : listeners) {
                reg.uninstallContribution(key, value);
            }
        }
    }

    public void fireResolved(Contribution<K, T> contrib) {
        K key = contrib.getId();
        T value = contrib.getValue();
        if (value == null) {
            throw new IllegalStateException("contribution is null");
        }
        installContribution(key, value);
        if (!listeners.isEmpty()) {
            for (AbstractContributionRegistry<K, T> reg : listeners) {
                reg.installContribution(key, value);
            }
        }
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
