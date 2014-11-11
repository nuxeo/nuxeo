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
     * Returns the initial state name.
     *
     * @return the initial state name
     */
    String getInitialStateName();

    /**
     * Gets the life cycle manager name.
     *
     * @return the life cycle manager name
     */
    String getLifeCycleManagerName();

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
