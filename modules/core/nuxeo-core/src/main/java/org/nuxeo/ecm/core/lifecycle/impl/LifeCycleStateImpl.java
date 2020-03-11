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

    public LifeCycleStateImpl(String name, String description, Collection<String> allowedStateTransitions,
            boolean initial) {
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
