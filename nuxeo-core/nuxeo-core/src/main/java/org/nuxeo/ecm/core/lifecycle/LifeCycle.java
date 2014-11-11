/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 * $Id: LifeCycle.java 21744 2007-07-02 12:51:51Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle;

import java.util.Collection;

/**
 * Document life cycle.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleImpl
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface LifeCycle {

    /**
     * Gets the allowed state transitions from a given state.
     *
     * @param stateName the current state name
     * @return collection of allowed state transition names.
     */
    Collection<String> getAllowedStateTransitionsFrom(String stateName)
            throws LifeCycleException;

    /**
     * Returns the default initial state name.
     */
    String getDefaultInitialStateName();

    /**
     * Returns the list of allowed initial state names.
     */
    Collection<String> getInitialStateNames();

    /**
     * Gets the life cycle name.
     *
     * @return the life cycle name
     */
    String getName();

    /**
     * Returns a life cycle state instance given its name.
     *
     * @param stateName the state name
     * @return the life cycle state instance
     */
    LifeCycleState getStateByName(String stateName);

    /**
     * Returns the list of life cycle state instances.
     *
     * @return the list of life cycle state instances
     */
    Collection<LifeCycleState> getStates();

    /**
     * Returns a life cycle transition instance given its name.
     *
     * @param transitionName the transition name
     * @return the life cycle transition instance
     */
    LifeCycleTransition getTransitionByName(String transitionName);

    /**
     * Returns a list of life cycle transition instances.
     *
     * @return a list of life cycle transition instances.
     */
    Collection<LifeCycleTransition> getTransitions();

}
