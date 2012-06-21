/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.lifecycle.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleDescriptor;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * Registry for life cycles
 *
 * @since 5.6
 */
public class LifeCycleRegistry extends
        ContributionFragmentRegistry<LifeCycleDescriptor> {

    private static final Log log = LogFactory.getLog(LifeCycleRegistry.class);

    protected Map<String, LifeCycle> lifeCycles = new HashMap<String, LifeCycle>();

    @Override
    public String getContributionId(LifeCycleDescriptor contrib) {
        return contrib.getName();
    }

    @Override
    public void contributionUpdated(String id, LifeCycleDescriptor contrib,
            LifeCycleDescriptor newOrigContrib) {
        log.info("Registering lifecycle: " + contrib.getName());
        lifeCycles.put(contrib.getName(), getLifeCycle(contrib));
    }

    @Override
    public void contributionRemoved(String id,
            LifeCycleDescriptor lifeCycleDescriptor) {
        log.info("Unregistering lifecycle: " + lifeCycleDescriptor.getName());
        lifeCycles.remove(lifeCycleDescriptor.getName());
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public LifeCycleDescriptor clone(LifeCycleDescriptor orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(LifeCycleDescriptor src, LifeCycleDescriptor dst) {
        throw new UnsupportedOperationException();
    }

    // API

    public LifeCycle getLifeCycle(String name) {
        return lifeCycles.get(name);
    }

    public Collection<LifeCycle> getLifeCycles() {
        return lifeCycles.values();
    }

    /**
     * Returns a life cycle instance out of the life cycle configuration.
     */
    public LifeCycle getLifeCycle(LifeCycleDescriptor desc) {
        String name = desc.getName();
        String initialStateName = desc.getInitialStateName();
        String defaultInitialStateName = desc.getDefaultInitialStateName();
        if (initialStateName != null) {
            defaultInitialStateName = initialStateName;
            log.warn(String.format("Lifecycle registration of default initial"
                    + " state has changed, change initial=\"%s\" to "
                    + "defaultInitial=\"%s\" in lifecyle '%s' definition",
                    defaultInitialStateName, defaultInitialStateName, name));
        }
        boolean defaultInitialStateFound = false;
        Collection<String> initialStateNames = new HashSet<String>();
        Collection<LifeCycleState> states = desc.getStates();
        for (LifeCycleState state : states) {
            String stateName = state.getName();
            if (defaultInitialStateName.equals(stateName)) {
                defaultInitialStateFound = true;
                initialStateNames.add(stateName);
            }
            if (state.isInitial()) {
                initialStateNames.add(stateName);
            }
        }
        if (!defaultInitialStateFound) {
            log.error(String.format(
                    "Default initial state %s not found on lifecycle %s",
                    defaultInitialStateName, name));
        }
        return new LifeCycleImpl(name, defaultInitialStateName,
                initialStateNames, states, desc.getTransitions());
    }

}
