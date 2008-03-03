/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 * $Id: LifeCycleImpl.java 19002 2007-05-20 15:37:19Z sfermigier $
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
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class LifeCycleImpl implements LifeCycle {

    /** The name of the life cycle. */
    private final String name;

    /** The life life cycle manager name for the life cycle. */
    private final String lifeCycleManagerName;

    /** The initial state name. */
    private final String initialStateName;

    /** The list of life cycle states. */
    private final Collection<LifeCycleState> states;

    /** The list of life cycle transition. */
    private final Collection<LifeCycleTransition> transitions;


    public LifeCycleImpl(String name, String lifeCycleManagerName,
            String initialStateName, Collection<LifeCycleState> states,
            Collection<LifeCycleTransition> transitions) {
        this.name = name;
        this.lifeCycleManagerName = lifeCycleManagerName;
        this.initialStateName = initialStateName;
        this.states = states;
        this.transitions = transitions;
    }

    public String getInitialStateName() {
        return initialStateName;
    }

    public String getLifeCycleManagerName() {
        return lifeCycleManagerName;
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

    public Collection<String> getAllowedStateTransitionsFrom(String state)
            throws LifeCycleException {
        LifeCycleState lifeCycleState = getStateByName(state);
        if (lifeCycleState != null) {
            return lifeCycleState.getAllowedStateTransitions();
        } else {
            throw new LifeCycleException("State <" + state
                    + "> does not exist !");
        }
    }

    public Collection<LifeCycleTransition> getTransitions() {
        return transitions;
    }

    public LifeCycleTransition getTransitionByName(String transition) {
        LifeCycleTransition lifeCycleTransition = null;
        for (LifeCycleTransition itransition : transitions) {
            if (itransition.getName().equals(transition)) {
                lifeCycleTransition = itransition;
                break;
            }
        }
        return lifeCycleTransition;
    }

}
