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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContributionImpl<K,T> implements Contribution<K,T> {

    private static final long serialVersionUID = 1L;


    protected AbstractRegistry<K,T> registry;
    protected K primaryKey;
    protected List<T> fragments = new ArrayList<T>();
    // the contributions I depend on
    protected Set<Contribution<K,T>> dependencies = new HashSet<Contribution<K,T>>();
    // the contributions that are waiting for me
    protected Set<Contribution<K,T>> dependents = new HashSet<Contribution<K,T>>();
    // the unresolved dependencies that are blocking my registration
    protected Set<Contribution<K,T>> unresolvedDependencies = new HashSet<Contribution<K,T>>();

    // last merged fragment
    protected T value = null;
    protected boolean isResolved = false;



    public ContributionImpl(AbstractRegistry<K,T> reg,  K primaryKey) {
        this.primaryKey = primaryKey;
        this.registry = reg;
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
        return unresolvedDependencies;
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
        if (fragments.remove(fragment)) {
            if (fragments.isEmpty()) {
                unregister();
            } else {
                update();
            }
            return true;
        }
        return false;
    }

    public void addFragment(T fragment, K ... superKeys) {
        fragments.add(fragment);
        if (superKeys != null && superKeys.length > 0) {
            if (superKeys != null && superKeys.length > 0) {
                for (int i=0; i<superKeys.length; i++) {
                    Contribution<K,T> c = registry.getOrCreateContribution(superKeys[i]);
                    dependencies.add(c);
                    c.getDependents().add(this);
                }
            }
        }
        // recompute resolved state
        update();
    }

    @SuppressWarnings("unchecked")
    public T merge() {
        try {
            if (fragments.isEmpty()) {
                return value;
            }
            if (!isResolved) {
                throw new IllegalStateException("Cannot compute merged values for not resolved contributions");
            }
            try {
                value = (T)fragments.get(0).getClass().newInstance();
            } catch (Exception e) {
                e.printStackTrace(); //TODO
                return null;
            }
            // first build object from its super objects if any
            for (Contribution<K,T> key : dependencies) {
                T superObject = (T)registry.getContribution(key.getId()).merge();
                registry.applyFragment(value, superObject);
            }
            // and now apply fragments
            for (T fragment : this) {
                registry.applyFragment(value, fragment);
            }
            return value;
        } catch (Exception e) {
            e.printStackTrace();
            return null; //TODO
        }
    }

    public boolean isPhantom() {
        return fragments.isEmpty();
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
        // update missing requirements
        unresolvedDependencies.clear();
        for (Contribution<K,T> c : dependencies) {
            if (!c.isResolved()) {
                unresolvedDependencies.add(c);
            }
        }
        boolean canResolve = unresolvedDependencies.isEmpty();
        if (isResolved != canResolve) { // resolved state changed
            if (canResolve) {
                resolve(null);
            } else {
                unresolve(null);
            }
        } else if (isResolved) {
            registry.fireUpdated(this);
        }
    }

    public void unregister() {
        if (isResolved) {
            unresolve(null);
        }
        fragments.clear();
        value = null;
    }

    public void unresolve(Contribution<K,T> requirement) {
        if (!isResolved) {
            return;
        }
        if (requirement != null) {
            unresolvedDependencies.add(requirement);
        }
        isResolved = false;
        for (Contribution<K,T> dep : dependents) {
            dep.unresolve(this);
        }
        registry.fireUnresolved(this);
        value = null;
    }

    public void resolve(Contribution<K,T> requirement) {
        if (isResolved || isPhantom()) {
            throw new IllegalStateException("Cannot resolve. Invalid state. phantom: "+isPhantom()+"; resolved: "+isResolved);
        }
        if (requirement != null) {
            unresolvedDependencies.remove(requirement);
        }
        if (unresolvedDependencies.isEmpty()) { // resolve dependents
            isResolved = true;
            registry.fireResolved(this);
            for (Contribution<K,T> dep : dependents) {
                if (!dep.isResolved()) {
                    dep.resolve(this);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Contribution) {
            return primaryKey.equals(((Contribution)obj).getId());
        }
        return false;
    }

    @Override
    public String toString() {
        return primaryKey.toString()+" [ phantom: "+isPhantom()+"; resolved: "+isResolved+"]";
    }
}
