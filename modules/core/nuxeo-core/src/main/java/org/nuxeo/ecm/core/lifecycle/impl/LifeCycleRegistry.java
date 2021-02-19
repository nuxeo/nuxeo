/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.core.lifecycle.impl;

import static java.util.function.Predicate.isEqual;
import static org.nuxeo.ecm.core.api.LifeCycleConstants.DELETED_STATE;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.core.lifecycle.extensions.LifeCycleDescriptor;

/**
 * Registry for life cycles
 *
 * @since 5.6
 */
public class LifeCycleRegistry extends MapRegistry<LifeCycleDescriptor> {

    private static final Logger log = LogManager.getLogger(LifeCycleRegistry.class);

    protected Map<String, LifeCycle> lifeCycles = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
        super.initialize();
        lifeCycles.clear();
        lifeCycles.putAll(getContributionValues().stream()
                                                 .collect(Collectors.toConcurrentMap(LifeCycleDescriptor::getName,
                                                         this::getLifeCycle)));
    }

    // API

    public LifeCycle getLifeCycle(String name) {
        if (name == null) {
            return null;
        }
        checkInitialized();
        return lifeCycles.get(name);
    }

    public Collection<LifeCycle> getLifeCycles() {
        checkInitialized();
        return lifeCycles.values();
    }

    /**
     * Returns a life cycle instance out of the life cycle configuration.
     */
    public LifeCycle getLifeCycle(LifeCycleDescriptor desc) {
        checkInitialized();
        String name = desc.getName();
        String initialStateName = desc.getInitialStateName();
        String defaultInitialStateName = desc.getDefaultInitialStateName();
        if (initialStateName != null) {
            defaultInitialStateName = initialStateName;
            log.warn(
                    "Lifecycle registration of default initial state has changed, change initial=\"{}\" to "
                            + "defaultInitial=\"{}\" in lifecyle '{}' definition",
                    defaultInitialStateName, defaultInitialStateName, name);
        }
        boolean defaultInitialStateFound = false;
        Collection<String> initialStateNames = new HashSet<>();
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
            log.error("Default initial state '{}' not found on lifecycle '{}'", defaultInitialStateName, name);
        }
        // look for "deleted" state to warn about usage
        if (!"default".equals(desc.getName())
                && desc.getStates().stream().map(LifeCycleState::getName).anyMatch(isEqual(DELETED_STATE))) {
            log.warn("The 'deleted' state is deprecated and shouldn't be use anymore."
                    + " Please consider removing it from you life cycle policy and use trash service instead.");
        }
        return new LifeCycleImpl(name, defaultInitialStateName, initialStateNames, states, desc.getTransitions());
    }

}
