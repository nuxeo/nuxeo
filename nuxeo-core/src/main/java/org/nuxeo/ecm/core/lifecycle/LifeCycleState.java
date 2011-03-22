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
 * $Id:LifeCycleState.java 4249 2006-10-16 19:56:10Z janguenot $
 */

package org.nuxeo.ecm.core.lifecycle;

import java.util.Collection;

/**
 * Life cycle state.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleStateImpl
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
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
