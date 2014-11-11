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

    @Override
    public Collection<String> getAllowedStateTransitions() {
        return allowedStateTransitions;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isInitial() {
        return initial;
    }

}
