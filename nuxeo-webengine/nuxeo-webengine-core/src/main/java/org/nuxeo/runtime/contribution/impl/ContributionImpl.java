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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.nuxeo.runtime.contribution.Contribution;
import org.nuxeo.runtime.contribution.ContributionRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContributionImpl<K, T> implements Contribution<K, T> {

    private static final Log log = LogFactory.getLog(ContributionImpl.class);

    protected final AbstractContributionRegistry<K, T> registry;
    protected final K primaryKey;
    protected final List<T> mainFragments = new ArrayList<T>();
    protected final List<T> fragments = new ArrayList<T>();

    // the contributions I depend on
    protected final Set<Contribution<K, T>> dependencies = new HashSet<Contribution<K, T>>();

    // the contributions that are waiting for me
    protected final Set<Contribution<K, T>> dependents = new HashSet<Contribution<K, T>>();

    // the unresolved dependencies that are blocking my registration
    //TODO: this member can be removed since we can obtain unresolved deps from dependencies set.
    //protected Set<Contribution<K,T>> unresolvedDependencies = new HashSet<Contribution<K,T>>();

    // last merged fragment
    protected T value;
    protected boolean isResolved = false;

    public ContributionImpl(AbstractContributionRegistry<K,T> reg,  K primaryKey) {
        this.primaryKey = primaryKey;
        registry = reg;
    }

    public ContributionRegistry<K,T> getRegistry() {
        return registry;
    }

    /**
     * @return the primaryKey.
     */
    public K getId() {
        return primaryKey;
    }

    public Iterator<T> iterator() {
        return fragments.iterator();
    }

    public Set<Contribution<K,T>> getDependencies() {
        return dependencies;
    }

    public Set<Contribution<K,T>> getDependents() {
        return dependents;
    }

    public Set<Contribution<K,T>> getUnresolvedDependencies() {
        Set<Contribution<K,T>> set = new HashSet<Contribution<K,T>>();
        for (Contribution<K,T> dep : dependencies) {
            if (dep.isResolved()) {
                set.add(dep);
            }
        }
        return set;
    }

    protected boolean checkIsResolved() {
        if (mainFragments.isEmpty()) {
            return false;
        }
        for (Contribution<K,T> dep : dependencies) {
            if (!dep.isResolved()) {
                return false;
            }
        }
        return true;
    }

    public int size() {
        return fragments.size();
    }

    public boolean isEmpty() {
        return fragments.isEmpty();
    }

    public T getFragment(int index) {
        return fragments.get(index);
    }

    public boolean removeFragment(Object fragment) {
        if (mainFragments.remove(fragment)) {
            if (mainFragments.isEmpty()) {
                if (fragments.isEmpty()) {
                    unregister();
                } else {
                    unresolve();
                }
            } else {
                update();
            }
            return true;
        }
        if (fragments.remove(fragment)) {
            if (!mainFragments.isEmpty()) {
                update();
            }
            return true;
        }
        return false;
    }

    public void addFragment(T fragment, K ... superKeys) {
        // check if it is the main fragment
        if (registry.isMainFragment(fragment)) {
            mainFragments.add(fragment);
        } else { // update contribution fragments
            fragments.add(fragment);
        }
        // when passing a null value as the superKey you get an array with a null element
        if (superKeys != null && superKeys.length > 0 && superKeys[0] != null) {
            for (K superKey : superKeys) {
                Contribution<K, T> c = registry.getOrCreateDependency(superKey);
                dependencies.add(c);
                c.getDependents().add(this);
            }
        }
        // recompute resolved state
        update();
    }

    public T getValue() {
        try {
            if (!isResolved) {
                throw new IllegalStateException(
                        "Cannot compute merged values for not resolved contributions");
            }
            if (mainFragments.isEmpty() || value != null) {
                return value;
            }
            // clone the last registered main fragment.
            T result = registry.clone(mainFragments.get(mainFragments.size()-1));
            // first apply its super objects if any
            for (Contribution<K,T> key : dependencies) {
                T superObject = registry.getContribution(key.getId()).getValue();
                registry.applySuperFragment(result, superObject);
            }
            // and now apply fragments
            for (T fragment : this) {
                registry.applyFragment(result, fragment);
            }
            value = result;
            return result;
        } catch (Exception e) {
            log.error(e);
            return null; //TODO
        }
    }

    public boolean isPhantom() {
        return mainFragments.isEmpty();
    }

    public boolean isResolved() {
        return isResolved;
    }

    public boolean isRegistered() {
        return !fragments.isEmpty();
    }

    /**
     * Called each time a fragment is added or removed
     * to update resolved state and to fire update notifications to
     * the registry owning that contribution
     */
    protected void update() {
        T oldValue = value;
        value = null;
        boolean canResolve = checkIsResolved();
        if (isResolved != canResolve) { // resolved state changed
            if (canResolve) {
                resolve();
            } else {
                unresolve();
            }
        } else if (isResolved) {
            registry.fireUpdated(oldValue, this);
        }
    }

    public void unregister() {
        if (isResolved) {
            unresolve();
        }
        fragments.clear();
        value = null;
    }

    public void unresolve() {
        if (!isResolved) {
            return;
        }
        isResolved = false;
        for (Contribution<K,T> dep : dependents) {
            dep.unresolve();
        }
        registry.fireUnresolved(this, value);
        value = null;
    }

    public void resolve() {
        if (isResolved || isPhantom()) {
            throw new IllegalStateException(
                    "Cannot resolve. Invalid state. phantom: "+isPhantom()+"; resolved: "+isResolved);
        }
        if (checkIsResolved()) { // resolve dependents
            isResolved = true;
            registry.fireResolved(this);
            for (Contribution<K,T> dep : dependents) {
                if (!dep.isResolved()) {
                    dep.resolve();
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Contribution) {
            return primaryKey.equals(((Contribution) obj).getId());
        }
        return false;
    }

    @Override
    public String toString() {
        return primaryKey.toString()+" [ phantom: "+isPhantom()+"; resolved: "+isResolved+"]";
    }

}
