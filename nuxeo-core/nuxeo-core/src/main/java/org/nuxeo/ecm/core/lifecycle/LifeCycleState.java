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

}
