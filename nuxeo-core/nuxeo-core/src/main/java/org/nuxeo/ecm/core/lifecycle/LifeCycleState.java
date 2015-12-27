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
 * $Id:LifeCycleState.java 4249 2006-10-16 19:56:10Z janguenot $
 */

package org.nuxeo.ecm.core.lifecycle;

import java.util.Collection;

/**
 * Life cycle state.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleStateImpl
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface LifeCycleState {

    /**
     * Returns the life cycle state name.
     *
     * @return the life cycle state name as a string
     */
    String getName();

    /**
     * Returns the life cycle state descriptions.
     *
     * @return the life cycle state description
     */
    String getDescription();

    /**
     * Returns the allowed state transitions.
     *
     * @return a collection of string representing the allowed state transitions
     */
    Collection<String> getAllowedStateTransitions();

    /**
     * Returns true if state is a valid initial state
     */
    boolean isInitial();

}
