/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 * $Id: LifeCycle.java 21744 2007-07-02 12:51:51Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle;

import java.util.Collection;

/**
 * Document life cycle.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleImpl
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface LifeCycle {

    /**
     * Gets the allowed state transitions from a given state.
     *
     * @param stateName the current state name
     * @return collection of allowed state transition names.
     */
    Collection<String> getAllowedStateTransitionsFrom(String stateName);

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
