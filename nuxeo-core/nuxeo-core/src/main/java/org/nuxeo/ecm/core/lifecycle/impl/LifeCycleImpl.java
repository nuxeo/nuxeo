/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Julien Anguenot
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.lifecycle.impl;

import java.util.Collection;
import java.util.Collections;

import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleException;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.core.lifecycle.LifeCycleTransition;

/**
 * Life cycle implementation.
 *
 * @see org.nuxeo.ecm.core.lifecycle.LifeCycle
 *
 * @author Julien Anguenot
 * @author Florent Guillaume
 */
public class LifeCycleImpl implements LifeCycle {

    private final String name;

    private final String defaultInitialStateName;

    private final Collection<String> initialStateNames;

    private final Collection<LifeCycleState> states;

    private final Collection<LifeCycleTransition> transitions;

    public LifeCycleImpl(String name, String defaultInitialStateName,
            Collection<String> initialStateNames,
            Collection<LifeCycleState> states,
            Collection<LifeCycleTransition> transitions) {
        this.name = name;
        this.defaultInitialStateName = defaultInitialStateName;
        this.initialStateNames = initialStateNames;
        this.states = states;
        this.transitions = transitions;
    }

    @Override
    public String getDefaultInitialStateName() {
        return defaultInitialStateName;
    }

    @Override
    public Collection<String> getInitialStateNames() {
        return initialStateNames;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<LifeCycleState> getStates() {
        return states;
    }

    @Override
    public LifeCycleState getStateByName(String stateName) {
        LifeCycleState lifeCycleState = null;
        for (LifeCycleState state : states) {
            if (state.getName().equals(stateName)) {
                lifeCycleState = state;
                break;
            }
        }
        return lifeCycleState;
    }

    @Override
    public Collection<String> getAllowedStateTransitionsFrom(String stateName)
            throws LifeCycleException {
        LifeCycleState lifeCycleState = getStateByName(stateName);
        if (lifeCycleState != null) {
            return lifeCycleState.getAllowedStateTransitions();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Collection<LifeCycleTransition> getTransitions() {
        return transitions;
    }

    @Override
    public LifeCycleTransition getTransitionByName(String transitionName) {
        LifeCycleTransition lifeCycleTransition = null;
        for (LifeCycleTransition itransition : transitions) {
            if (itransition.getName().equals(transitionName)) {
                lifeCycleTransition = itransition;
                break;
            }
        }
        return lifeCycleTransition;
    }

}
