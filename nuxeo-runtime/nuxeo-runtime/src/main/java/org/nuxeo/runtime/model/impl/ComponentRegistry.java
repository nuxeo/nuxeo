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
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RuntimeModelException;
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
    protected final Map<ComponentName, RegistrationInfoImpl> components = new HashMap<ComponentName, RegistrationInfoImpl>();

    /** Map of aliased name to canonical name. */
    protected final Map<ComponentName, ComponentName> aliases = new HashMap<ComponentName, ComponentName>();

    protected final MappedSet<ComponentName, RegistrationInfo> requiredPendings = new MappedSet<ComponentName, RegistrationInfo>();

    protected final ComponentManagerImpl manager;

    protected ComponentRegistry(ComponentManagerImpl owner) {
        manager = owner;
    }

    public void destroy() {
        components.clear();
        aliases.clear();
        requiredPendings.clear();
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
     *
     * @param ri
     * @return true if the component was resolved, false if the component is
     *         pending
     */
    @SuppressWarnings("unchecked")
    public boolean addComponent(RegistrationInfoImpl ri) throws RuntimeModelException {
        // update registry
        ComponentName name = ri.getName();
        Set<ComponentName> al = ri.getAliases();
        components.put(name, ri);
        for (ComponentName n : al) {
            aliases.put(n, name);
        }
        if (al != null && !al.isEmpty()) {
            log.info("Aliasing component: " + name + " -> " + al);
        }

        for (ComponentName pending : ri.requiredPendings) {
            requiredPendings.put(pending, ri);
        }

        // set state
        Set<RegistrationInfo> dependsOnMe = requiredPendings.remove(name);

        ri.register((Set<? extends RegistrationInfoImpl>) dependsOnMe);

        return ri.isResolved();
    }

    public RegistrationInfoImpl removeComponent(ComponentName name)
            throws Exception {
        // update registry
        RegistrationInfoImpl ri = components.remove(name);
        if (ri == null) {
            return null;
        }
        for (ComponentName alias : ri.aliases) {
            aliases.remove(alias);
        }
        if (ri.isResolved()) {
            for (ComponentName pending : ri.requiredPendings) {
                requiredPendings.remove(pending, ri);
            }
        }
        // update state
        try {
            ri.unresolve();
        } finally {
            ri.unregister();
        }
        return ri;
    }

    public Set<ComponentName> getMissingDependencies(ComponentName name) {
        return components.get(unaliased(name)).requiredPendings;
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

}
