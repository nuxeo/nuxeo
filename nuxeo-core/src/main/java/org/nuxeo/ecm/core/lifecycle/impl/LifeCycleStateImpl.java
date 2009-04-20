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
 * $Id: LifeCycleStateImpl.java 19002 2007-05-20 15:37:19Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle.impl;

import java.util.Collection;
import java.util.Collections;

import org.nuxeo.ecm.core.lifecycle.LifeCycleState;

/**
 * Life cycle state implementation.
 *
 * @see org.nuxeo.ecm.core.lifecycle.LifeCycleState
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class LifeCycleStateImpl implements LifeCycleState {

    /** Name of the life cycle state. */
    private final String name;

    /** State description. */
    private final String description;

    /** Collection of allowed state transitions. */
    private final Collection<String> allowedStateTransitions;

    private final boolean initial;

    public LifeCycleStateImpl(String name, String description,
            Collection<String> allowedStateTransitions, boolean initial) {
        this.name = name;
        this.description = description;
        this.allowedStateTransitions = Collections.unmodifiableCollection(allowedStateTransitions);
        this.initial = initial;
    }

    public Collection<String> getAllowedStateTransitions() {
        return allowedStateTransitions;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public boolean isInitial() {
        return initial;
    }

}
