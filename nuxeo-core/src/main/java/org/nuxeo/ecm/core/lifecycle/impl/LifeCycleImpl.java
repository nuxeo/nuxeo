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
 *     Julien Anguenot
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.lifecycle.impl;

import java.util.Collection;

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

    @Deprecated
    public String getInitialStateName() {
        return getDefaultInitialStateName();
    }

    public String getDefaultInitialStateName() {
        return defaultInitialStateName;
    }

    public Collection<String> getInitialStateNames() {
        return initialStateNames;
    }

    public String getName() {
        return name;
    }

    public Collection<LifeCycleState> getStates() {
        return states;
    }

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

    public Collection<String> getAllowedStateTransitionsFrom(String stateName)
            throws LifeCycleException {
        LifeCycleState lifeCycleState = getStateByName(stateName);
        if (lifeCycleState != null) {
            return lifeCycleState.getAllowedStateTransitions();
        } else {
            throw new LifeCycleException("State <" + stateName
                    + "> does not exist !");
        }
    }

    public Collection<LifeCycleTransition> getTransitions() {
        return transitions;
    }

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
